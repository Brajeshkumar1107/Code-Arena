package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.execution.process.ProcessExecutor;
import com.codexsphere.codearena.execution.process.ProcessExecutorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultProcessExecutorFactory
        implements ProcessExecutorFactory {

    private final LocalProcessExecutor localProcessExecutor;
    private final DockerProcessExecutor dockerProcessExecutor;
    private final RunnerProperties runnerProperties;

    @Override
    public ProcessExecutor getExecutor() {

        if (runnerProperties.getEnvironment()
                == ExecutionEnvironmentType.DOCKER) {

            return dockerProcessExecutor;

        }

        return localProcessExecutor;

    }

}