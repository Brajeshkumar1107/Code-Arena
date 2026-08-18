package com.codexsphere.codearena.execution.docker.resource;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.execution.docker.config.DockerProperties;
import com.github.dockerjava.api.model.HostConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultResourceConfigurer
        implements ResourceConfigurer {

    private final RunnerProperties runnerProperties;

    @Override
    public void configure(
            HostConfig hostConfig
    ) {

        DockerProperties dockerProperties =
                runnerProperties.getDocker();

        hostConfig
                .withMemory(
                        dockerProperties.getMemoryMb()
                                * 1024L
                                * 1024L
                )
                .withNanoCPUs(
                        (long) (
                                dockerProperties.getCpuCount()
                                        * 1_000_000_000L
                        )
                )
                .withPidsLimit(
                        dockerProperties.getPidsLimit()
                )
                .withTmpFs(
                        Map.of(
                                "/tmp",
                                "size="
                                        + dockerProperties.getTmpfsSizeMb()
                                        + "m"
                        )
                );
    }
}