package com.codexsphere.codearena.execution.docker.service.impl;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.DockerExecutionException;
import com.codexsphere.codearena.exception.ProcessExecutionException;
import com.codexsphere.codearena.execution.docker.callback.DockerResultCallback;
import com.codexsphere.codearena.execution.docker.factory.DockerContainerFactory;
import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DockerServiceImplTest {

    private DockerClient dockerClient;

    private DockerContainerFactory dockerContainerFactory;

    private DockerServiceImpl dockerService;

    private DockerContainer container;

    private LanguageMetadata metadata;

    private Path workspace;

    private RunnerProperties runnerProperties;

    @BeforeEach
    void setUp() {

        dockerClient =
                mock(
                        DockerClient.class,
                        RETURNS_DEEP_STUBS
                );

        dockerContainerFactory =
                mock(DockerContainerFactory.class);

        runnerProperties =
                mock(
                        RunnerProperties.class,
                        RETURNS_DEEP_STUBS
                );

        dockerService =
                new DockerServiceImpl(
                        dockerClient,
                        dockerContainerFactory,
                        runnerProperties
                );

        container =
                mock(DockerContainer.class);

        metadata =
                mock(LanguageMetadata.class);

        workspace =
                Path.of("/tmp/workspace");

        when(
                container.getId()
        ).thenReturn("container-123");

        when(
                runnerProperties
                        .getOutputLimits()
                        .getMaxStdoutBytes()
        ).thenReturn(1024L);

        when(
                runnerProperties
                        .getOutputLimits()
                        .getMaxStderrBytes()
        ).thenReturn(1024L);

        when(
                runnerProperties
                        .getDocker()
                        .getMemoryMb()
        ).thenReturn(512L);
    }

    // =========================================================
    // CREATE CONTAINER
    // =========================================================

    @Test
    void shouldCreateContainerSuccessfully() {

        when(
                dockerContainerFactory.create(
                        metadata,
                        workspace
                )
        ).thenReturn(container);

        DockerContainer result =
                dockerService.createContainer(
                        metadata,
                        workspace
                );

        assertSame(
                container,
                result
        );

        verify(
                dockerContainerFactory
        ).create(
                metadata,
                workspace
        );
    }

    @Test
    void shouldRethrowDockerExecutionExceptionWhenCreatingContainer() {

        DockerExecutionException exception =
                new DockerExecutionException(
                        "Docker creation failed."
                );

        when(
                dockerContainerFactory.create(
                        metadata,
                        workspace
                )
        ).thenThrow(exception);

        DockerExecutionException thrown =
                assertThrows(
                        DockerExecutionException.class,
                        () ->
                                dockerService.createContainer(
                                        metadata,
                                        workspace
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verify(
                dockerContainerFactory
        ).create(
                metadata,
                workspace
        );
    }

    @Test
    void shouldWrapUnexpectedExceptionWhenCreatingContainer() {

        RuntimeException cause =
                new RuntimeException(
                        "Docker unavailable"
                );

        when(
                dockerContainerFactory.create(
                        metadata,
                        workspace
                )
        ).thenThrow(cause);

        DockerExecutionException thrown =
                assertThrows(
                        DockerExecutionException.class,
                        () ->
                                dockerService.createContainer(
                                        metadata,
                                        workspace
                                )
                );

        assertEquals(
                "Unable to create Docker container.",
                thrown.getMessage()
        );

        assertSame(
                cause,
                thrown.getCause()
        );
    }

    // =========================================================
    // START CONTAINER
    // =========================================================

    @Test
    void shouldStartContainerSuccessfully() {

        StartContainerCmd startCommand =
                mock(StartContainerCmd.class);

        when(
                dockerClient.startContainerCmd(
                        "container-123"
                )
        ).thenReturn(startCommand);

        doNothing()
                .when(startCommand)
                .exec();

        dockerService.startContainer(
                container
        );

        verify(
                dockerClient
        ).startContainerCmd(
                "container-123"
        );

        verify(
                startCommand
        ).exec();

        verify(
                container
        ).setRunning(true);
    }

    @Test
    void shouldThrowDockerExecutionExceptionWhenStartFails() {

        StartContainerCmd startCommand =
                mock(StartContainerCmd.class);

        RuntimeException cause =
                new RuntimeException(
                        "Docker start failed"
                );

        when(
                dockerClient.startContainerCmd(
                        "container-123"
                )
        ).thenReturn(startCommand);

        when(
                startCommand.exec()
        ).thenThrow(cause);

        DockerExecutionException thrown =
                assertThrows(
                        DockerExecutionException.class,
                        () ->
                                dockerService.startContainer(
                                        container
                                )
                );

        assertEquals(
                "Unable to start Docker container.",
                thrown.getMessage()
        );

        assertSame(
                cause,
                thrown.getCause()
        );

        verify(
                container,
                never()
        ).setRunning(true);
    }

    // =========================================================
    // STOP CONTAINER
    // =========================================================

    @Test
    void shouldIgnoreNullContainerWhenStopping() {

        assertDoesNotThrow(
                () ->
                        dockerService.stopContainer(
                                null
                        )
        );

        verifyNoInteractions(
                dockerClient
        );
    }

    @Test
    void shouldIgnoreContainerWithoutIdWhenStopping() {

        DockerContainer containerWithoutId =
                mock(DockerContainer.class);

        when(
                containerWithoutId.getId()
        ).thenReturn(null);

        assertDoesNotThrow(
                () ->
                        dockerService.stopContainer(
                                containerWithoutId
                        )
        );

        verifyNoInteractions(
                dockerClient
        );
    }

    @Test
    void shouldStopContainerSuccessfully() {

        StopContainerCmd stopCommand =
                mock(StopContainerCmd.class);

        when(
                dockerClient.stopContainerCmd(
                        "container-123"
                )
        ).thenReturn(stopCommand);

        when(
                stopCommand.withTimeout(1)
        ).thenReturn(stopCommand);

        doNothing()
                .when(stopCommand)
                .exec();

        dockerService.stopContainer(
                container
        );

        verify(
                dockerClient
        ).stopContainerCmd(
                "container-123"
        );

        verify(
                stopCommand
        ).withTimeout(1);

        verify(
                stopCommand
        ).exec();

        verify(
                container
        ).setRunning(false);
    }

    @Test
    void shouldHandleAlreadyStoppedContainer() {

        StopContainerCmd stopCommand =
                mock(StopContainerCmd.class);

        when(
                dockerClient.stopContainerCmd(
                        "container-123"
                )
        ).thenReturn(stopCommand);

        when(
                stopCommand.withTimeout(1)
        ).thenReturn(stopCommand);

        when(
                stopCommand.exec()
        ).thenThrow(
                mock(
                        com.github.dockerjava.api.exception
                                .NotModifiedException.class
                )
        );

        assertDoesNotThrow(
                () ->
                        dockerService.stopContainer(
                                container
                        )
        );

        verify(
                container
        ).setRunning(false);
    }

    @Test
    void shouldHandleMissingContainerWhenStopping() {

        StopContainerCmd stopCommand =
                mock(StopContainerCmd.class);

        when(
                dockerClient.stopContainerCmd(
                        "container-123"
                )
        ).thenReturn(stopCommand);

        when(
                stopCommand.withTimeout(1)
        ).thenReturn(stopCommand);

        when(
                stopCommand.exec()
        ).thenThrow(
                mock(
                        jakarta.ws.rs.NotFoundException.class
                )
        );

        assertDoesNotThrow(
                () ->
                        dockerService.stopContainer(
                                container
                        )
        );

        verify(
                container
        ).setRunning(false);
    }

    @Test
    void shouldNotThrowWhenStoppingContainerFails() {

        StopContainerCmd stopCommand =
                mock(StopContainerCmd.class);

        when(
                dockerClient.stopContainerCmd(
                        "container-123"
                )
        ).thenReturn(stopCommand);

        when(
                stopCommand.withTimeout(1)
        ).thenReturn(stopCommand);

        when(
                stopCommand.exec()
        ).thenThrow(
                new RuntimeException(
                        "Docker unavailable"
                )
        );

        assertDoesNotThrow(
                () ->
                        dockerService.stopContainer(
                                container
                        )
        );

        verify(
                container,
                never()
        ).setRunning(false);
    }

    // =========================================================
    // REMOVE CONTAINER
    // =========================================================

    @Test
    void shouldIgnoreNullContainerWhenRemoving() {

        assertDoesNotThrow(
                () ->
                        dockerService.removeContainer(
                                null
                        )
        );

        verifyNoInteractions(
                dockerClient
        );
    }

    @Test
    void shouldIgnoreContainerWithoutIdWhenRemoving() {

        DockerContainer containerWithoutId =
                mock(DockerContainer.class);

        when(
                containerWithoutId.getId()
        ).thenReturn(null);

        assertDoesNotThrow(
                () ->
                        dockerService.removeContainer(
                                containerWithoutId
                        )
        );

        verifyNoInteractions(
                dockerClient
        );
    }

    @Test
    void shouldRemoveContainerSuccessfully() {

        RemoveContainerCmd removeCommand =
                mock(RemoveContainerCmd.class);

        when(
                dockerClient.removeContainerCmd(
                        "container-123"
                )
        ).thenReturn(removeCommand);

        when(
                removeCommand.withForce(true)
        ).thenReturn(removeCommand);

        doNothing()
                .when(removeCommand)
                .exec();

        dockerService.removeContainer(
                container
        );

        verify(
                dockerClient
        ).removeContainerCmd(
                "container-123"
        );

        verify(
                removeCommand
        ).withForce(true);

        verify(
                removeCommand
        ).exec();
    }

    @Test
    void shouldNotThrowWhenRemovingContainerFails() {

        RemoveContainerCmd removeCommand =
                mock(RemoveContainerCmd.class);

        when(
                dockerClient.removeContainerCmd(
                        "container-123"
                )
        ).thenReturn(removeCommand);

        when(
                removeCommand.withForce(true)
        ).thenReturn(removeCommand);

        when(
                removeCommand.exec()
        ).thenThrow(
                new RuntimeException(
                        "Docker unavailable"
                )
        );

        assertDoesNotThrow(
                () ->
                        dockerService.removeContainer(
                                container
                        )
        );

        verify(
                removeCommand
        ).exec();
    }

    // =========================================================
    // EXECUTE
    // =========================================================

    @Test
    void shouldExecuteProcessSuccessfully()
            throws Exception {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "python",
                                        "main.py"
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(2)
                        )
                        .build();

        configureSuccessfulDockerExecution(
                0L,
                false
        );

        ProcessResult result =
                dockerService.execute(
                        "container-123",
                        request
                );

        assertFalse(
                result.isTimeout()
        );

        assertFalse(
                result.isMemoryLimitExceeded()
        );

        assertFalse(
                result.isOutputLimitExceeded()
        );

        assertEquals(
                0,
                result.getExitCode()
        );

        /*
         * No onNext() is used.
         *
         * The callback remains empty because this test
         * verifies DockerServiceImpl execution flow,
         * not DockerResultCallback output parsing.
         */
        assertEquals(
                "",
                result.getStdout()
        );

        assertEquals(
                "",
                result.getStderr()
        );

        assertNotNull(
                result.getExecutionTimeMillis()
        );

        verify(
                dockerClient
        ).statsCmd(
                "container-123"
        );

        verify(
                dockerClient
        ).execCreateCmd(
                "container-123"
        );

        verify(
                dockerClient
        ).execStartCmd(
                "exec-123"
        );
    }

    @Test
    void shouldReturnMinusOneWhenExitCodeIsNull()
            throws Exception {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "python",
                                        "main.py"
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(2)
                        )
                        .build();

        configureSuccessfulDockerExecution(
                null,
                false
        );

        ProcessResult result =
                dockerService.execute(
                        "container-123",
                        request
                );

        assertEquals(
                -1,
                result.getExitCode()
        );
    }

    @Test
    void shouldReportMemoryLimitExceededWhenContainerIsOomKilled()
            throws Exception {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "python",
                                        "memory.py"
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(2)
                        )
                        .build();

        configureSuccessfulDockerExecution(
                137L,
                true
        );

        ProcessResult result =
                dockerService.execute(
                        "container-123",
                        request
                );

        assertTrue(
                result.isMemoryLimitExceeded()
        );

        assertEquals(
                512L,
                result.getMemoryUsedMb()
        );

        assertEquals(
                137,
                result.getExitCode()
        );

        assertFalse(
                result.isTimeout()
        );

        assertFalse(
                result.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldReportTimeoutWhenExecutionExceedsDeadline()
            throws Exception {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "python",
                                        "main.py"
                                )
                        )
                        .timeout(
                                Duration.ofMillis(500)
                        )
                        .build();

        configureDockerExecutionForTimeout();

        ProcessResult result =
                dockerService.execute(
                        "container-123",
                        request
                );

        assertTrue(
                result.isTimeout()
        );

        assertEquals(
                -1,
                result.getExitCode()
        );

        assertFalse(
                result.isMemoryLimitExceeded()
        );

        assertFalse(
                result.isOutputLimitExceeded()
        );

        assertNotNull(
                result.getExecutionTimeMillis()
        );

        assertTrue(
                result.getStderr()
                        .contains(
                                "Process execution timed out."
                        )
        );

        verify(
                dockerClient
        ).killContainerCmd(
                "container-123"
        );
    }

    private void configureSuccessfulDockerExecution() {

        ExecStartCmd execStartCmd =
                mock(ExecStartCmd.class);

        when(
                dockerClient.execStartCmd("exec-123")
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withDetach(false)
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withTty(false)
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withStdIn(any())
        ).thenReturn(execStartCmd);

        doAnswer(invocation -> {

            DockerResultCallback callback =
                    invocation.getArgument(
                            0,
                            DockerResultCallback.class
                    );

            callback.close();

            return callback;

        }).when(execStartCmd)
                .exec(any(DockerResultCallback.class));
    }


    @Test
    void shouldThrowProcessExecutionExceptionWhenDockerExecFails() {

        ProcessRequest request =
                ProcessRequest.builder()
                        .command(
                                List.of(
                                        "python",
                                        "main.py"
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(2)
                        )
                        .build();

        StatsCmd statsCmd =
                mock(StatsCmd.class);

        when(
                dockerClient.statsCmd(
                        "container-123"
                )
        ).thenReturn(statsCmd);

        when(
                statsCmd.withNoStream(false)
        ).thenReturn(statsCmd);

        doAnswer(
                invocation ->
                        invocation.getArgument(0)
        ).when(statsCmd).exec(any());

        ExecCreateCmd execCreateCmd =
                mock(ExecCreateCmd.class);

        when(
                dockerClient.execCreateCmd(
                        "container-123"
                )
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStdout(true)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStderr(true)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStdin(false)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withTty(false)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withCmd(
                        any(String[].class)
                )
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.exec()
        ).thenThrow(
                new RuntimeException(
                        "Docker exec failed"
                )
        );

        ProcessExecutionException exception =
                assertThrows(
                        ProcessExecutionException.class,
                        () ->
                                dockerService.execute(
                                        "container-123",
                                        request
                                )
                );

        assertEquals(
                "Failed to execute command inside Docker.",
                exception.getMessage()
        );

        verify(
                dockerClient
        ).execCreateCmd(
                "container-123"
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void configureSuccessfulDockerExecution(
            Long exitCode,
            boolean oomKilled
    ) {

        StatsCmd statsCmd =
                mock(StatsCmd.class);

        when(
                dockerClient.statsCmd(
                        "container-123"
                )
        ).thenReturn(statsCmd);

        when(
                statsCmd.withNoStream(false)
        ).thenReturn(statsCmd);

        doAnswer(
                invocation ->
                        invocation.getArgument(0)
        ).when(statsCmd).exec(any());

        ExecCreateCmd execCreateCmd =
                mock(ExecCreateCmd.class);

        ExecCreateCmdResponse execResponse =
                mock(ExecCreateCmdResponse.class);

        when(
                execResponse.getId()
        ).thenReturn("exec-123");

        when(
                dockerClient.execCreateCmd(
                        "container-123"
                )
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStdout(true)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStderr(true)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStdin(anyBoolean())
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withTty(false)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withCmd(
                        any(String[].class)
                )
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.exec()
        ).thenReturn(execResponse);

        ExecStartCmd execStartCmd =
                mock(ExecStartCmd.class);

        when(
                dockerClient.execStartCmd(
                        "exec-123"
                )
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withDetach(false)
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withTty(false)
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withStdIn(any())
        ).thenReturn(execStartCmd);

        /*
         * DockerServiceImpl calls:
         *
         * exec(callback).awaitCompletion()
         *
         * We signal callback completion directly.
         *
         * IMPORTANT:
         * We intentionally do NOT call onNext().
         */
        doAnswer(invocation -> {

            DockerResultCallback callback =
                    invocation.getArgument(
                            0,
                            DockerResultCallback.class
                    );

            callback.close();

            return callback;

        }).when(execStartCmd).exec(
                any(DockerResultCallback.class)
        );

        /*
         * Inspect exec exit code.
         *
         * No explicit InspectContainerResponse
         * type is required.
         */
        InspectExecResponse inspectExecResponse =
                mock(InspectExecResponse.class);

        when(
                dockerClient
                        .inspectExecCmd("exec-123")
                        .exec()
        ).thenReturn(
                inspectExecResponse
        );

        when(
                inspectExecResponse.getExitCodeLong()
        ).thenReturn(exitCode);

        /*
         * Inspect container OOM state.
         *
         * Deep stubbing avoids depending on the
         * Docker Java nested ContainerState type.
         */
        when(
                dockerClient
                        .inspectContainerCmd("container-123")
                        .exec()
                        .getState()
                        .getOOMKilled()
        ).thenReturn(
                oomKilled
        );
    }

    private void configureDockerExecutionForTimeout() {

        StatsCmd statsCmd =
                mock(StatsCmd.class);

        when(
                dockerClient.statsCmd(
                        "container-123"
                )
        ).thenReturn(statsCmd);

        when(
                statsCmd.withNoStream(false)
        ).thenReturn(statsCmd);

        doAnswer(
                invocation ->
                        invocation.getArgument(0)
        ).when(statsCmd).exec(any());

        ExecCreateCmd execCreateCmd =
                mock(ExecCreateCmd.class);

        ExecCreateCmdResponse execResponse =
                mock(ExecCreateCmdResponse.class);

        when(
                execResponse.getId()
        ).thenReturn("exec-123");

        when(
                dockerClient.execCreateCmd(
                        "container-123"
                )
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStdout(true)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStderr(true)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withAttachStdin(false)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withTty(false)
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.withCmd(
                        any(String[].class)
                )
        ).thenReturn(execCreateCmd);

        when(
                execCreateCmd.exec()
        ).thenReturn(execResponse);

        ExecStartCmd execStartCmd =
                mock(ExecStartCmd.class);

        when(
                dockerClient.execStartCmd(
                        "exec-123"
                )
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withDetach(false)
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withTty(false)
        ).thenReturn(execStartCmd);

        when(
                execStartCmd.withStdIn(any())
        ).thenReturn(execStartCmd);

        /*
         * Keep the Docker task running longer than
         * the configured execution timeout.
         *
         * No onNext() call.
         */
        doAnswer(
                invocation -> {

                    /*
                     * Keep Docker execution running longer than
                     * the configured timeout.
                     *
                     * We intentionally do NOT call onNext().
                     */
                    Thread.sleep(5000);

                    return invocation.getArgument(0);

                }
        ).when(execStartCmd).exec(
                any(ResultCallback.class)
        );

        /*
         * killContainer() internally executes:
         *
         * dockerClient
         *      .killContainerCmd(containerId)
         *      .withSignal("SIGKILL")
         *      .exec();
         */
        com.github.dockerjava.api.command.KillContainerCmd
                killCommand =
                mock(
                        com.github.dockerjava.api.command.KillContainerCmd.class
                );

        when(
                dockerClient.killContainerCmd(
                        "container-123"
                )
        ).thenReturn(killCommand);

        when(
                killCommand.withSignal(
                        "SIGKILL"
                )
        ).thenReturn(killCommand);

        doNothing()
                .when(killCommand)
                .exec();
    }
}