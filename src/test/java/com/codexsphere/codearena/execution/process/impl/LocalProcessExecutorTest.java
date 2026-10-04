package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.exception.ProcessExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LocalProcessExecutorTest {

    private static final int ONE_MEBIBYTE =
            1024 * 1024;

    private LocalProcessExecutor executor;

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void setUp() {

        executor =
                new LocalProcessExecutor();
    }

    /*
     * Builds a command that runs ProcessFixture through the current JVM.
     *
     * Invoking the fixture rather than "sh -c" keeps the suite portable: the
     * executor is OS-agnostic, so the tests should not depend on a POSIX
     * shell being installed. The classpath comes from the surefire/IDE test
     * JVM, which already has ProcessFixture on it.
     *
     * JAVA_TOOL_OPTIONS is cleared on the child because the JVM prints
     * "Picked up JAVA_TOOL_OPTIONS: ..." to stderr on startup, which would
     * otherwise contaminate every stderr assertion.
     */
    private List<String> fixture(String... args) {

        String javaBinary =
                Path.of(
                                System.getProperty(
                                        "java.home"
                                ),
                                "bin",
                                isWindows()
                                        ? "java.exe"
                                        : "java"
                        )
                        .toString();

        List<String> command =
                new ArrayList<>();

        command.add(javaBinary);

        /*
         * Pinned so the fixture's own stdout encoding does not depend on
         * the ambient JAVA_TOOL_OPTIONS the test JVM inherited.
         */
        command.add(
                "-Dfile.encoding=UTF-8"
        );

        command.add(
                "-cp"
        );

        command.add(
                System.getProperty(
                        "java.class.path"
                )
        );

        command.add(
                ProcessFixture.class
                        .getName()
        );

        command.addAll(
                List.of(args)
        );

        return command;
    }

    private static boolean isWindows() {

        return System.getProperty("os.name")
                .toLowerCase()
                .contains("win");
    }

    /*
     * Strips the JVM launcher's "Picked up JAVA_TOOL_OPTIONS: ..." notice.
     *
     * The notice is written to stderr by the launcher before main() runs
     * whenever JAVA_TOOL_OPTIONS is set in the environment. The child JVM
     * inherits it from the test JVM, and LocalProcessExecutor does not
     * override the child environment, so the line lands in the captured
     * stderr and breaks exact-match assertions. It is launcher noise, not
     * output from the process under test.
     */
    private static final Pattern LAUNCHER_BANNER =
            Pattern.compile(
                    "^Picked up (?:JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|JDK_JAVA_OPTIONS):[^\\r\\n]*\\R?",
                    Pattern.MULTILINE
            );

    private static String withoutLauncherBanner(String captured) {

        if (captured == null) {

            return null;
        }

        return LAUNCHER_BANNER
                .matcher(captured)
                .replaceAll(
                        ""
                );
    }

    private static String stderrOf(ProcessResult result) {

        return withoutLauncherBanner(
                result.getStderr()
        );
    }

    private static String stdoutOf(ProcessResult result) {

        return withoutLauncherBanner(
                result.getStdout()
        );
    }

    @Test
    void shouldExecuteSimpleCommandSuccessfully() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "out",
                                        "Hello World"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
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
                stdoutOf(result)
        );

        assertEquals(
                "",
                stderrOf(result)
        );
    }

    @Test
    void shouldCaptureStandardOutput() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "lines",
                                        "line1",
                                        "line2"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "line1\nline2\n",
                stdoutOf(result)
        );
    }

    @Test
    void shouldCaptureStandardError() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "err",
                                        "error message"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "",
                stdoutOf(result)
        );

        assertEquals(
                "error message",
                stderrOf(result)
        );
    }

    @Test
    void shouldCaptureBothStandardOutputAndStandardError() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "both",
                                        "stdout",
                                        "stderr"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "stdout",
                stdoutOf(result)
        );

        assertEquals(
                "stderr",
                stderrOf(result)
        );
    }

    @Test
    void shouldReturnNonZeroExitCodeForFailedProcess() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "fail",
                                        "7",
                                        "failure"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
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
                stderrOf(result)
        );
    }

    @Test
    void shouldPassInputToProcess() {

        String input =
                "Hello from stdin";

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture("cat")
                        )
                        .workingDirectory(tempDirectory)
                        .input(
                                new ByteArrayInputStream(
                                        input.getBytes(
                                                StandardCharsets.UTF_8
                                        )
                                )
                        )
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                input,
                stdoutOf(result)
        );
    }

    @Test
    void shouldCloseStdinWhenInputIsNotProvided() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture("cat")
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "",
                stdoutOf(result)
        );
    }

    @Test
    void shouldUseConfiguredWorkingDirectory()
            throws Exception {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture("pwd")
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        /*
         * Resolved rather than compared literally: on Windows the canonical
         * path may differ in case or 8.3 short form from the @TempDir value.
         */
        Path actual =
                Path.of(
                        stdoutOf(result)
                                .trim()
                )
                        .toRealPath();

        assertEquals(
                tempDirectory.toRealPath(),
                actual
        );
    }

    @Test
    void shouldReturnTimeoutWhenProcessExceedsTimeout() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "sleep",
                                        "5000"
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
                stderrOf(result)
        );

        /*
         * Make sure the timeout did not turn into
         * a five-second process execution.
         *
         * The upper bound is generous because this now destroys a JVM
         * rather than a sleep(1) builtin.
         */
        assertTrue(
                elapsed < 4000,
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
                                fixture(
                                        "sleep",
                                        "150"
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
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
                                fixture(
                                        "bigout",
                                        String.valueOf(
                                                ONE_MEBIBYTE
                                        )
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(30))
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
                ONE_MEBIBYTE,
                stdoutOf(result)
                        .getBytes(StandardCharsets.UTF_8)
                        .length
        );
    }

    @Test
    void shouldHandleLargeStandardErrorWithoutDeadlock() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture(
                                        "bigerr",
                                        String.valueOf(
                                                ONE_MEBIBYTE
                                        )
                                )
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(30))
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
                ONE_MEBIBYTE,
                stderrOf(result)
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
                        .timeout(Duration.ofSeconds(15))
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
                                fixture("cat")
                        )
                        .workingDirectory(tempDirectory)
                        .input(
                                new ByteArrayInputStream(
                                        new byte[0]
                                )
                        )
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                "",
                stdoutOf(result)
        );
    }

    @Test
    void shouldPreserveUnicodeOutput() {

        String expected =
                ProcessFixture.UNICODE_SAMPLE;

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                fixture("unicode")
                        )
                        .workingDirectory(tempDirectory)
                        .timeout(Duration.ofSeconds(15))
                        .build();

        ProcessResult result =
                executor.execute(request);

        assertEquals(
                0,
                result.getExitCode()
        );

        assertEquals(
                expected,
                stdoutOf(result)
        );
    }
}
