package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.exception.ProcessExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalProcessExecutorTest {

    private LocalProcessExecutor executor;

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void setUp() {

        executor =
                new LocalProcessExecutor();
    }

    @Test
    void shouldExecuteSimpleCommandSuccessfully() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "printf 'Hello World'"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertFalse(
                result.isTimeout()
        );

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "Hello World",
                result.getStdout()
        );

        assertEquals(
                "",
                result.getStderr()
        );
    }

    @Test
    void shouldCaptureStandardOutput() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "printf 'line1\\nline2\\n'"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "line1\nline2\n",
                result.getStdout()
        );
    }

    @Test
    void shouldCaptureStandardError() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "printf 'error message' >&2"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "",
                result.getStdout()
        );

        assertEquals(
                "error message",
                result.getStderr()
        );
    }

    @Test
    void shouldCaptureBothStandardOutputAndStandardError() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "printf 'stdout'; printf 'stderr' >&2"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "stdout",
                result.getStdout()
        );

        assertEquals(
                "stderr",
                result.getStderr()
        );
    }

    @Test
    void shouldReturnNonZeroExitCodeForFailedProcess() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "printf 'failure' >&2; exit 7"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertFalse(
                result.isTimeout()
        );

        assertEquals(
                7,
                result.getExitCode()
        );

        assertEquals(
                "failure",
                result.getStderr()
        );
    }

    @Test
    void shouldPassInputToProcess() {

        String input =
                "Hello from stdin";

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "cat"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .input(
                                new ByteArrayInputStream(
                                        input.getBytes(
                                                StandardCharsets.UTF_8
                                        )
                                )
                        )
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                input,
                result.getStdout()
        );
    }

    @Test
    void shouldCloseStdinWhenInputIsNotProvided() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "cat"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "",
                result.getStdout()
        );
    }

    @Test
    void shouldUseConfiguredWorkingDirectory() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "pwd"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        String actualDirectory =
                result.getStdout().trim();

        assertEquals(
                tempDirectory.toAbsolutePath().toString(),
                actualDirectory
        );
    }

    @Test
    void shouldReturnTimeoutWhenProcessExceedsTimeout() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "sleep 5"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(
                                Duration.ofMillis(200)
                        )
                        .build();

        long start =
                System.currentTimeMillis();

        ProcessResult result =
                executor.execute(request);

        long elapsed =
                System.currentTimeMillis() - start;

        assertTrue(
                result.isTimeout()
        );

        assertEquals(
                -1,
                result.getExitCode()
        );

        assertEquals(
                "Process execution timed out.",
                result.getStderr()
        );

        /*
         * Make sure the timeout did not turn into
         * a five-second process execution.
         */
        assertTrue(
                elapsed < 3000,
                "Process took too long after timeout: "
                        + elapsed
                        + " ms"
        );
    }

    @Test
    void shouldRecordExecutionTime() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "sleep 0.1"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertTrue(
                result.getExecutionTimeMillis() >= 50,
                "Expected execution time to be recorded"
        );
    }

    @Test
    void shouldHandleLargeStandardOutputWithoutDeadlock() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "yes 1234567890 | head -c 1048576"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertFalse(
                result.isTimeout()
        );

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                1024 * 1024,
                result.getStdout()
                        .getBytes(StandardCharsets.UTF_8)
                        .length
        );
    }

    @Test
    void shouldHandleLargeStandardErrorWithoutDeadlock() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "head -c 1048576 /dev/zero >&2"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertFalse(
                result.isTimeout()
        );

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                1024 * 1024,
                result.getStderr()
                        .getBytes(StandardCharsets.UTF_8)
                        .length
        );
    }
    @Test
    void shouldThrowProcessExecutionExceptionForInvalidCommand() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "command-that-does-not-exist-12345"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessExecutionException exception =
                assertThrows(
                        ProcessExecutionException.class,
                        () ->
                                executor.execute(request)
                );

        assertEquals(
                "Unable to execute local process.",
                exception.getMessage()
        );

        assertNotNull(
                exception.getCause()
        );
    }

    @Test
    void shouldHandleEmptyInput() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "cat"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .input(
                                new ByteArrayInputStream(
                                        new byte[0]
                                )
                        )
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "",
                result.getStdout()
        );
    }

    @Test
    void shouldPreserveUnicodeOutput() {

        String expected =
                "Hello 世界 नमस्ते 🚀";

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "sh",
                                        "-c",
                                        "printf '" + expected + "'"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(5))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                expected,
                result.getStdout()
        );
    }
}