package com.codexsphere.codearena.execution.docker.factory;

import com.codexsphere.codearena.execution.docker.host.HostConfigBuilder;
import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDockerContainerFactory
        implements DockerContainerFactory {

    private static final String CONTAINER_USER = "1000:1000";

    private final HostConfigBuilder hostConfigBuilder;
    private final DockerClient dockerClient;

    private void ensureImageExists(String image)
            throws InterruptedException {

        try {

            dockerClient.inspectImageCmd(image).exec();

            log.info(
                    "Docker image already available: {}",
                    image
            );

        } catch (NotFoundException ex) {

            log.info(
                    "Docker image not found. Pulling: {}",
                    image
            );

            dockerClient.pullImageCmd(image)
                    .start()
                    .awaitCompletion();

            log.info(
                    "Docker image pulled successfully: {}",
                    image
            );
        }
    }

    @Override
    public DockerContainer create(
            LanguageMetadata metadata,
            Path workspace
    ) {

        HostConfig hostConfig =
                hostConfigBuilder.build(workspace);

        String image =
                metadata.getDockerImage();

        try {

            ensureImageExists(image);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Docker image pull interrupted: " + image,
                    ex
            );
        }

        CreateContainerResponse response =
                dockerClient.createContainerCmd(image)
                        .withHostConfig(hostConfig)
                        .withUser(CONTAINER_USER)
                        .withWorkingDir("/workspace")
                        .withCmd(
                                "tail",
                                "-f",
                                "/dev/null"
                        )
                        .exec();

        log.info(
                "Docker container created: {} using user {}",
                response.getId(),
                CONTAINER_USER
        );

        return DockerContainer.builder()
                .id(response.getId())
                .image(image)
                .workspace(workspace)
                .running(false)
                .build();
    }
}