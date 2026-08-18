package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.exception.ProcessExecutionException;
import com.codexsphere.codearena.execution.process.ProcessExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LocalProcessExecutor implements ProcessExecutor {

    @Override
    public ProcessResult execute(ProcessRequest request) {

        long start = System.currentTimeMillis();

        var ref = new Object() {
            Process process = null;
        };

        ExecutorService streamExecutor = null;

        try {

            log.info(
                    "Executing local command: {}",
                    String.join(" ", request.getCommand())
            );

            ProcessBuilder processBuilder =
                    new ProcessBuilder(request.getCommand());

            processBuilder.directory(
                    request.getWorkingDirectory().toFile()
            );

            log.debug(
                    "Working directory: {}",
                    request.getWorkingDirectory()
            );

            ref.process = processBuilder.start();

            /*
             * Read stdout and stderr concurrently.
             *
             * This prevents the child process from blocking
             * when either output buffer becomes full.
             */
            streamExecutor =
                    Executors.newFixedThreadPool(2);

            var finalRef = ref;
            Future<String> stdoutFuture =
                    streamExecutor.submit(
                            () -> readStream(
                                    finalRef.process.getInputStream()
                            )
                    );

            Future<String> stderrFuture =
                    streamExecutor.submit(
                            () -> readStream(
                                    ref.process.getErrorStream()
                            )
                    );

            /*
             * Write stdin if provided.
             */
            if (request.getInput() != null) {

                log.debug(
                        "Writing stdin to local process."
                );

                request.getInput().transferTo(
                        ref.process.getOutputStream()
                );

                ref.process.getOutputStream().flush();
                ref.process.getOutputStream().close();

                log.debug(
                        "Local process stdin closed."
                );
            } else {

                /*
                 * Close stdin when there is no input.
                 *
                 * This is important for programs that wait for EOF.
                 */
                ref.process.getOutputStream().close();
            }

            /*
             * Wait for process completion with timeout.
             */
            boolean completed =
                    ref.process.waitFor(
                            request.getTimeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    );

            long executionTimeMillis =
                    System.currentTimeMillis() - start;

            /*
             * TIME LIMIT EXCEEDED
             */
            if (!completed) {

                log.warn(
                        "Local process timed out after {} ms.",
                        executionTimeMillis
                );

                ref.process.destroy();

                /*
                 * Give the process a short grace period
                 * before forcing termination.
                 */
                if (!ref.process.waitFor(
                        500,
                        TimeUnit.MILLISECONDS
                )) {

                    log.warn(
                            "Local process did not terminate gracefully. " +
                                    "Destroying forcibly."
                    );

                    ref.process.destroyForcibly();

                    ref.process.waitFor(
                            500,
                            TimeUnit.MILLISECONDS
                    );
                }

                return ProcessResult.builder()
                        .timeout(true)
                        .exitCode(-1)
                        .stdout("")
                        .stderr(
                                "Process execution timed out."
                        )
                        .executionTimeMillis(
                                executionTimeMillis
                        )
                        .build();
            }

            /*
             * Process completed normally.
             */
            int exitCode =
                    ref.process.exitValue();

            /*
             * Collect stdout/stderr.
             *
             * The readers have already been consuming
             * the streams while the process was running.
             */
            String stdout =
                    stdoutFuture.get();

            String stderr =
                    stderrFuture.get();

            log.info(
                    "Local process completed with exit code {}",
                    exitCode
            );

            log.debug(
                    "Local process execution time: {} ms",
                    executionTimeMillis
            );

            return ProcessResult.builder()
                    .exitCode(exitCode)
                    .stdout(stdout)
                    .stderr(stderr)
                    .timeout(false)
                    .executionTimeMillis(
                            executionTimeMillis
                    )
                    .build();

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            if (ref.process != null && ref.process.isAlive()) {
                ref.process.destroyForcibly();
            }

            throw new ProcessExecutionException(
                    "Process execution interrupted.",
                    ex
            );

        } catch (Exception ex) {

            if (ref.process != null && ref.process.isAlive()) {
                ref.process.destroyForcibly();
            }

            log.error(
                    "Local process execution failed.",
                    ex
            );

            throw new ProcessExecutionException(
                    "Unable to execute local process.",
                    ex
            );

        } finally {

            /*
             * Shut down stream reader threads.
             */
            if (streamExecutor != null) {

                streamExecutor.shutdownNow();

                try {

                    if (!streamExecutor.awaitTermination(
                            1,
                            TimeUnit.SECONDS
                    )) {

                        log.debug(
                                "Stream executor did not terminate cleanly."
                        );
                    }

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                    log.debug(
                            "Interrupted while shutting down " +
                                    "stream executor.",
                            ex
                    );
                }
            }

            /*
             * Make sure the process cannot remain alive
             * after this method returns.
             */
            if (ref.process != null && ref.process.isAlive()) {

                log.warn(
                        "Process still alive during cleanup. " +
                                "Destroying forcibly."
                );

                ref.process.destroyForcibly();
            }

            /*
             * Close stdin if it was supplied.
             */
            if (request.getInput() != null) {

                try {

                    request.getInput().close();

                } catch (IOException ex) {

                    log.debug(
                            "Unable to close process input stream.",
                            ex
                    );
                }
            }
        }
    }

    /**
     * Reads a process output stream completely.
     *
     * This method runs in a dedicated thread so stdout and
     * stderr can be consumed concurrently.
     */
    private String readStream(
            InputStream inputStream
    ) throws IOException {

        return new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
        );
    }
}