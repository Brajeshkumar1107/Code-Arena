package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.execution.process.ProcessExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultProcessExecutorFactoryTest {

    private LocalProcessExecutor localProcessExecutor;
    private DockerProcessExecutor dockerProcessExecutor;
    private RunnerProperties runnerProperties;

    private DefaultProcessExecutorFactory factory;

    @BeforeEach
    void setUp() {

        localProcessExecutor =
                mock(LocalProcessExecutor.class);

        dockerProcessExecutor =
                mock(DockerProcessExecutor.class);

        runnerProperties =
                mock(RunnerProperties.class);

        factory =
                new DefaultProcessExecutorFactory(
                        localProcessExecutor,
                        dockerProcessExecutor,
                        runnerProperties
                );
    }

    @Test
    void shouldReturnLocalProcessExecutorForLocalEnvironment() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.LOCAL
        );

        ProcessExecutor executor =
                factory.getExecutor();

        assertSame(
                localProcessExecutor,
                executor
        );
    }

    @Test
    void shouldReturnDockerProcessExecutorForDockerEnvironment() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.DOCKER
        );

        ProcessExecutor executor =
                factory.getExecutor();

        assertSame(
                dockerProcessExecutor,
                executor
        );
    }

    @Test
    void shouldReturnLocalExecutorWhenEnvironmentIsNull() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(null);

        ProcessExecutor executor =
                factory.getExecutor();

        assertSame(
                localProcessExecutor,
                executor
        );
    }

    @Test
    void shouldReadEnvironmentFromRunnerProperties() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.LOCAL
        );

        factory.getExecutor();

        verify(
                runnerProperties,
                times(1)
        ).getEnvironment();
    }

    @Test
    void shouldNotInteractWithExecutorsWhileSelectingExecutor() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.DOCKER
        );

        ProcessExecutor executor =
                factory.getExecutor();

        assertSame(
                dockerProcessExecutor,
                executor
        );

        verifyNoInteractions(
                localProcessExecutor
        );

        verifyNoInteractions(
                dockerProcessExecutor
        );
    }
}