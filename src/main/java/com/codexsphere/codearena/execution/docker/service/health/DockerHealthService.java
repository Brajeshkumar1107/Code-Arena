package com.codexsphere.codearena.execution.docker.service.health;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.github.dockerjava.api.DockerClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerHealthService {

    private final DockerClient dockerClient;
    private final RunnerProperties runnerProperties;

    @PostConstruct
    public void checkDocker() {

        if (runnerProperties.getEnvironment()
                != ExecutionEnvironmentType.DOCKER) {

            log.info(
                    "Docker health check skipped. environment={}",
                    runnerProperties.getEnvironment()
            );

            return;
        }

        try {

            var info = dockerClient.infoCmd().exec();

            log.info("Docker Connected Successfully");
            log.info("Docker Version : {}", info.getServerVersion());
            log.info("Containers     : {}", info.getContainers());
            log.info("Images         : {}", info.getImages());

        } catch (Exception exception) {

            log.warn(
                    "Docker health check failed. "
                            + "Runner will continue starting; "
                            + "executions will fail until Docker is available.",
                    exception
            );
        }
    }

}
