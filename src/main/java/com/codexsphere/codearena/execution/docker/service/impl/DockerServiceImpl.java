package com.codexsphere.codearena.execution.docker.service.impl;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.DockerExecutionException;
import com.codexsphere.codearena.exception.ProcessExecutionException;
import com.codexsphere.codearena.execution.docker.callback.DockerResultCallback;
import com.codexsphere.codearena.execution.docker.factory.DockerContainerFactory;
import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.docker.service.DockerService;
import com.codexsphere.codearena.execution.docker.stats.DockerMemoryStatsCallback;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerServiceImpl implements DockerService {

    private final DockerClient dockerClient;

    private final DockerContainerFactory dockerContainerFactory;

    private final RunnerProperties runnerProperties;

    @Override
    public DockerContainer createContainer(
            LanguageMetadata metadata,
            Path workspace
    ) {

        try {

            return dockerContainerFactory.create(
                    metadata,
                    workspace
            );

        } catch (DockerExecutionException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new DockerExecutionException(
                    "Unable to create Docker container.",
                    ex
            );
        }
    }

    @Override
    public void startContainer(
            DockerContainer container
    ) {

        try {

            dockerClient
                    .startContainerCmd(
                            container.getId()
                    )
                    .exec();

            container.setRunning(true);

        } catch (Exception ex) {

            throw new DockerExecutionException(
                    "Unable to start Docker container.",
                    ex
            );
        }
    }

    @Override
    public void stopContainer(
            DockerContainer container
    ) {

        if (container == null ||
                container.getId() == null) {

            return;
        }

        try {

            log.info(
                    "Stopping container: {}",
                    container.getId()
            );

            dockerClient.stopContainerCmd(
                            container.getId()
                    )
                    .withTimeout(1)
                    .exec();

            container.setRunning(false);

            log.info(
                    "Container stopped: {}",
                    container.getId()
            );

        } catch (NotModifiedException ex) {

            container.setRunning(false);

            log.debug(
                    "Container {} is already stopped.",
                    container.getId()
            );

        } catch (NotFoundException ex) {

            container.setRunning(false);

            log.debug(
                    "Container {} no longer exists.",
                    container.getId()
            );

        } catch (Exception ex) {

            log.warn(
                    "Failed to stop container {}.",
                    container.getId(),
                    ex
            );
        }
    }

    @Override
    public void removeContainer(
            DockerContainer container
    ) {

        if (container == null ||
                container.getId() == null) {

            return;
        }

        try {

            log.info(
                    "Removing container: {}",
                    container.getId()
            );

            dockerClient.removeContainerCmd(
                            container.getId()
                    )
                    .withForce(true)
                    .exec();

            log.info(
                    "Container removed: {}",
                    container.getId()
            );

        } catch (Exception ex) {

            log.warn(
                    "Failed to remove container {}.",
                    container.getId(),
                    ex
            );
        }
    }

    @Override
    public ProcessResult execute(
            String containerId,
            ProcessRequest request
    ) {

        long startTime =
                System.currentTimeMillis();

        /*
         * Output limits.
         */
        long maxStdoutBytes =
                runnerProperties
                        .getOutputLimits()
                        .getMaxStdoutBytes();

        long maxStderrBytes =
                runnerProperties
                        .getOutputLimits()
                        .getMaxStderrBytes();

        DockerResultCallback callback =
                new DockerResultCallback(
                        maxStdoutBytes,
                        maxStderrBytes
                );

        /*
         * Memory monitoring.
         */
        DockerMemoryStatsCallback memoryCallback =
                new DockerMemoryStatsCallback();

        /*
         * Executor used only to allow us to
         * monitor the Docker execution while
         * DockerResultCallback receives output.
         */
        ExecutorService executionExecutor =
                Executors.newSingleThreadExecutor(
                        runnable -> {

                            Thread thread =
                                    new Thread(
                                            runnable,
                                            "docker-exec-" + containerId
                                    );

                            thread.setDaemon(true);

                            return thread;
                        }
                );

        Future<?> executionFuture = null;

        try {

            log.info(
                    "Executing in container {}: {}",
                    containerId,
                    String.join(
                            " ",
                            request.getCommand()
                    )
            );

            log.info(
                    "Docker stdin present: {}",
                    request.getInput() != null
            );

            /*
             * Read input before creating Docker exec.
             */
            byte[] inputBytes = null;

            if (request.getInput() != null) {

                inputBytes =
                        request.getInput()
                                .readAllBytes();

                log.debug(
                        "Docker stdin size: {} bytes",
                        inputBytes.length
                );
            }

            InputStream dockerInput =
                    inputBytes == null
                            ? null
                            : new ByteArrayInputStream(
                            inputBytes
                    );

            /*
             * Configured Docker memory limit.
             */
            long configuredMemoryMb =
                    runnerProperties
                            .getDocker()
                            .getMemoryMb();

            /*
             * Start memory monitoring.
             */
            dockerClient.statsCmd(containerId)
                    .withNoStream(false)
                    .exec(memoryCallback);

            /*
             * Wait for the first memory sample.
             */
            memoryCallback.awaitFirstSample(100);

            /*
             * Create Docker exec process.
             */
            ExecCreateCmdResponse exec =
                    dockerClient.execCreateCmd(
                                    containerId
                            )
                            .withAttachStdout(true)
                            .withAttachStderr(true)
                            .withAttachStdin(
                                    dockerInput != null
                            )
                            .withTty(false)
                            .withCmd(
                                    request.getCommand()
                                            .toArray(
                                                    new String[0]
                                            )
                            )
                            .exec();

            log.debug(
                    "Docker exec created: {}",
                    exec.getId()
            );

            /*
             * Start execution asynchronously.
             *
             * IMPORTANT:
             *
             * Future<?> is used because Docker's
             * awaitCompletion() does not return a
             * Boolean value.
             */
            executionFuture =
                    executionExecutor.submit(
                            () -> {

                                try {

                                    dockerClient
                                            .execStartCmd(
                                                    exec.getId()
                                            )
                                            .withDetach(false)
                                            .withTty(false)
                                            .withStdIn(
                                                    dockerInput
                                            )
                                            .exec(callback)
                                            .awaitCompletion();

                                } catch (InterruptedException ex) {

                                    Thread.currentThread()
                                            .interrupt();

                                    throw new RuntimeException(
                                            "Docker execution interrupted.",
                                            ex
                                    );
                                }
                            }
                    );

            /*
             * Execution timeout.
             */
            long timeoutMillis =
                    request.getTimeout()
                            .toMillis();

            long deadline =
                    System.nanoTime()
                            + TimeUnit.MILLISECONDS
                            .toNanos(timeoutMillis);

            boolean completed = false;
            boolean timedOut = false;
            boolean outputLimitExceeded = false;

            /*
             * Monitor Docker execution.
             */
            while (true) {

                if (callback.isOutputLimitExceeded()) {

                    outputLimitExceeded = true;

                    log.warn(
                            "Container {} exceeded output limit. Killing execution.",
                            containerId
                    );

                    killContainer(containerId);

                    break;
                }

                if (executionFuture.isDone()) {

                    try {
                        executionFuture.get();
                    } catch (Exception ignored) {
                        // Expected if Docker execution was interrupted/killed.
                    }

                    /*
                     * Check one final time because Docker output
                     * collection is asynchronous.
                     */
                    if (callback.isOutputLimitExceeded()) {

                        outputLimitExceeded = true;

                        log.warn(
                                "Container {} exceeded output limit.",
                                containerId
                        );

                        break;
                    }

                    completed = true;
                    break;
                }

                if (System.nanoTime() >= deadline) {

                    log.warn(
                            "Container {} execution timed out. Killing execution.",
                            containerId
                    );

                    timedOut = true;

                    killContainer(containerId);

                    break;
                }

                Thread.sleep(10);
            }

            long executionTime =
                    System.currentTimeMillis()
                            - startTime;

            /*
             * Give Docker a short amount of time
             * to finish after being killed.
             */
            if (!completed &&
                    executionFuture != null) {

                try {

                    executionFuture.get(
                            500,
                            TimeUnit.MILLISECONDS
                    );

                    completed = true;

                } catch (Exception ignored) {

                    /*
                     * Expected when the Docker
                     * process was killed.
                     */
                }
            }

            /*
             * Read peak memory.
             */
            long sampledMemoryMb =
                    memoryCallback.getPeakMemoryMb();

            log.info(
                    "Container {} peak sampled memory: {} MB",
                    containerId,
                    sampledMemoryMb
            );

            /*
             * OUTPUT LIMIT EXCEEDED
             */
            if (outputLimitExceeded) {

                return ProcessResult.builder()
                        .timeout(false)
                        .exitCode(-1)
                        .stdout(
                                callback.getStdout()
                        )
                        .stderr(
                                appendOutputLimitMessage(
                                        callback.getStderr()
                                )
                        )
                        .executionTimeMillis(
                                executionTime
                        )
                        .memoryUsedMb(
                                sampledMemoryMb
                        )
                        .memoryLimitExceeded(false)
                        .outputLimitExceeded(true)
                        .build();
            }

            /*
             * TIME LIMIT EXCEEDED
             */
            if (timedOut) {

                return ProcessResult.builder()
                        .timeout(true)
                        .exitCode(-1)
                        .stdout(
                                callback.getStdout()
                        )
                        .stderr(
                                appendTimeoutMessage(
                                        callback.getStderr()
                                )
                        )
                        .executionTimeMillis(
                                executionTime
                        )
                        .memoryUsedMb(
                                sampledMemoryMb
                        )
                        .memoryLimitExceeded(false)
                        .outputLimitExceeded(false)
                        .build();
            }

            /*
             * Check whether the asynchronous
             * execution failed.
             */
            try {

                executionFuture.get();

            } catch (ExecutionException ex) {

                Throwable cause =
                        ex.getCause();

                log.error(
                        "Docker execution task failed.",
                        cause
                );

                throw new ProcessExecutionException(
                        "Docker process execution failed.",
                        cause
                );
            }

            /*
             * Get process exit code.
             */
            Long exitCodeLong =
                    dockerClient
                            .inspectExecCmd(
                                    exec.getId()
                            )
                            .exec()
                            .getExitCodeLong();

            int exitCode =
                    exitCodeLong == null
                            ? -1
                            : exitCodeLong.intValue();

            log.info(
                    "Container {} finished with exit code {}",
                    containerId,
                    exitCode
            );

            /*
             * Check Docker OOM state.
             */
            boolean oomKilled =
                    dockerClient
                            .inspectContainerCmd(
                                    containerId
                            )
                            .exec()
                            .getState()
                            .getOOMKilled();

            /*
             * If Docker killed the process because
             * of memory exhaustion, the configured
             * memory limit is the meaningful value.
             */
            long reportedMemoryMb =
                    oomKilled
                            ? configuredMemoryMb
                            : sampledMemoryMb;

            if (oomKilled) {

                log.warn(
                        "Container {} exceeded memory limit. " +
                                "Configured limit: {} MB",
                        containerId,
                        configuredMemoryMb
                );
            }

            /*
             * Final process result.
             */
            return ProcessResult.builder()
                    .stdout(
                            callback.getStdout()
                    )
                    .stderr(
                            callback.getStderr()
                    )
                    .exitCode(exitCode)
                    .executionTimeMillis(
                            executionTime
                    )
                    .timeout(false)
                    .memoryUsedMb(
                            reportedMemoryMb
                    )
                    .memoryLimitExceeded(
                            oomKilled
                    )
                    .outputLimitExceeded(false)
                    .build();

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            log.error(
                    "Docker execution interrupted.",
                    ex
            );

            if (executionFuture != null) {

                executionFuture.cancel(true);
            }

            killContainer(containerId);

            throw new ProcessExecutionException(
                    "Docker process execution interrupted.",
                    ex
            );

        } catch (ProcessExecutionException ex) {

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Docker execution failed.",
                    ex
            );

            killContainer(containerId);

            throw new ProcessExecutionException(
                    "Failed to execute command inside Docker.",
                    ex
            );

        } finally {

            /*
             * Cancel monitoring task if still running.
             */
            if (executionFuture != null &&
                    !executionFuture.isDone()) {

                executionFuture.cancel(true);
            }

            executionExecutor.shutdownNow();

            /*
             * Close stdout/stderr callback.
             */
            try {

                callback.close();

            } catch (Exception ex) {

                log.debug(
                        "Unable to close Docker result callback.",
                        ex
                );
            }

            /*
             * Close memory statistics callback.
             */
            try {

                memoryCallback.close();

            } catch (Exception ex) {

                log.debug(
                        "Unable to close Docker memory stats callback.",
                        ex
                );
            }

            /*
             * Close original request input.
             */
            try {

                if (request.getInput() != null) {

                    request.getInput().close();
                }

            } catch (Exception ex) {

                log.debug(
                        "Unable to close Docker stdin.",
                        ex
                );
            }
        }
    }

    /**
     * Kill a running Docker container.
     */
    private void killContainer(
            String containerId
    ) {

        if (containerId == null ||
                containerId.isBlank()) {

            return;
        }

        try {

            dockerClient
                    .killContainerCmd(
                            containerId
                    )
                    .withSignal("SIGKILL")
                    .exec();

            log.info(
                    "Container {} killed.",
                    containerId
            );

        } catch (NotFoundException ex) {

            log.debug(
                    "Container {} no longer exists.",
                    containerId
            );

        } catch (Exception ex) {

            log.warn(
                    "Failed to kill container {}.",
                    containerId,
                    ex
            );
        }
    }

    private String appendTimeoutMessage(
            String stderr
    ) {

        if (stderr == null ||
                stderr.isBlank()) {

            return "Process execution timed out.";
        }

        return stderr
                + System.lineSeparator()
                + "Process execution timed out.";
    }

    private String appendOutputLimitMessage(
            String stderr
    ) {

        if (stderr == null ||
                stderr.isBlank()) {

            return "Output limit exceeded.";
        }

        return stderr
                + System.lineSeparator()
                + "Output limit exceeded.";
    }
}