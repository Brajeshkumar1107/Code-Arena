package com.codexsphere.codearena.execution.docker.service.health;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.github.dockerjava.api.DockerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DockerHealthIndicator
        implements HealthIndicator {

    private final DockerClient dockerClient;
    private final RunnerProperties runnerProperties;

    @Override
    public Health health() {

        if (runnerProperties.getEnvironment()
                != ExecutionEnvironmentType.DOCKER) {

            return Health.up()
                    .withDetail(
                            "environment",
                            "LOCAL"
                    )
                    .build();
        }

        try {

            var info = dockerClient.infoCmd().exec();

            return Health.up()
                    .withDetail(
                            "dockerVersion",
                            info.getServerVersion()
                    )
                    .withDetail(
                            "containers",
                            info.getContainers()
                    )
                    .withDetail(
                            "images",
                            info.getImages()
                    )
                    .build();

        } catch (Exception exception) {

            return Health.down()
                    .withException(exception)
                    .build();
        }
    }
}
