package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.exception.DockerExecutionException;
import com.codexsphere.codearena.exception.ProcessExecutionException;
import com.codexsphere.codearena.execution.docker.service.DockerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DockerProcessExecutorTest {

    private DockerService dockerService;

    private DockerProcessExecutor executor;

    private ProcessRequest request;

    @BeforeEach
    void setUp() {

        dockerService =
                mock(DockerService.class);

        request =
                mock(ProcessRequest.class);

        executor =
                new DockerProcessExecutor(
                        dockerService
                );

        when(
                request.getContainerId()
        ).thenReturn("container-123");
    }

    @Test
    void shouldReturnProcessResultWhenDockerExecutionSucceeds() {

        ProcessResult expectedResult =
                mock(ProcessResult.class);

        when(
                dockerService.execute(
                        "container-123",
                        request
                )
        ).thenReturn(expectedResult);

        ProcessResult actualResult =
                executor.execute(request);

        assertSame(
                expectedResult,
                actualResult
        );

        verify(
                dockerService
        ).execute(
                "container-123",
                request
        );
    }

    @Test
    void shouldRethrowDockerExecutionException() {

        DockerExecutionException exception =
                new DockerExecutionException(
                        "Docker execution failed."
                );

        when(
                dockerService.execute(
                        "container-123",
                        request
                )
        ).thenThrow(exception);

        DockerExecutionException thrown =
                assertThrows(
                        DockerExecutionException.class,
                        () ->
                                executor.execute(request)
                );

        assertSame(
                exception,
                thrown
        );

        assertEquals(
                "Docker execution failed.",
                thrown.getMessage()
        );

        verify(
                dockerService
        ).execute(
                "container-123",
                request
        );
    }

    @Test
    void shouldWrapProcessExecutionException() {

        ProcessExecutionException cause =
                new ProcessExecutionException(
                        "Process execution failed.",
                        new RuntimeException(
                                "Underlying process error"
                        )
                );

        when(
                dockerService.execute(
                        "container-123",
                        request
                )
        ).thenThrow(cause);

        DockerExecutionException thrown =
                assertThrows(
                        DockerExecutionException.class,
                        () ->
                                executor.execute(request)
                );

        assertEquals(
                "Unable to execute process in Docker.",
                thrown.getMessage()
        );

        assertSame(
                cause,
                thrown.getCause()
        );

        verify(
                dockerService
        ).execute(
                "container-123",
                request
        );
    }

    @Test
    void shouldWrapUnexpectedException() {

        RuntimeException cause =
                new RuntimeException(
                        "Unexpected Docker failure"
                );

        when(
                dockerService.execute(
                        "container-123",
                        request
                )
        ).thenThrow(cause);

        DockerExecutionException thrown =
                assertThrows(
                        DockerExecutionException.class,
                        () ->
                                executor.execute(request)
                );

        assertEquals(
                "Unable to execute process in Docker.",
                thrown.getMessage()
        );

        assertSame(
                cause,
                thrown.getCause()
        );

        verify(
                dockerService
        ).execute(
                "container-123",
                request
        );
    }

    @Test
    void shouldPassContainerIdAndRequestToDockerService() {

        ProcessResult result =
                mock(ProcessResult.class);

        when(
                dockerService.execute(
                        "container-123",
                        request
                )
        ).thenReturn(result);

        executor.execute(request);

        verify(
                dockerService,
                times(1)
        ).execute(
                "container-123",
                request
        );

        verifyNoMoreInteractions(
                dockerService
        );
    }

    @Test
    void shouldHandleNullContainerId() {

        ProcessResult result =
                mock(ProcessResult.class);

        when(
                request.getContainerId()
        ).thenReturn(null);

        when(
                dockerService.execute(
                        null,
                        request
                )
        ).thenReturn(result);

        ProcessResult actualResult =
                executor.execute(request);

        assertSame(
                result,
                actualResult
        );

        verify(
                dockerService
        ).execute(
                null,
                request
        );
    }
}