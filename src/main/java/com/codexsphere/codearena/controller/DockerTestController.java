package com.codexsphere.codearena.controller;

import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.docker.service.DockerService;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.language.LanguageMetadataFactory;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/docker")
@Profile("dev")
public class DockerTestController {

    private final DockerService dockerService;
    private final LanguageMetadataFactory metadataFactory;

    public DockerTestController(DockerService dockerService, LanguageMetadataFactory metadataFactory) {
        this.dockerService = dockerService;
        this.metadataFactory = metadataFactory;
    }

    @GetMapping("/test")
    public String test() throws Exception {

        Path workspace =
                Files.createTempDirectory("docker-test-");

        LanguageMetadata metadata =
                metadataFactory.get(Language.JAVA);

        DockerContainer container =
                dockerService.createContainer(
                        metadata,
                        workspace
                );

        dockerService.startContainer(container);

        ProcessRequest request =
                ProcessRequest.builder()
                        .containerId(container.getId())
                        .command(
                                List.of(
                                        "java",
                                        "-version"
                                )
                        )
                        .timeout(Duration.ofSeconds(10))
                        .build();

        ProcessResult result =
                dockerService.execute(
                        container.getId(),
                        request
                );

        dockerService.removeContainer(container);

        return """
                STDOUT:
                %s

                STDERR:
                %s
                """.formatted(
                result.getStdout(),
                result.getStderr()
        );
    }
}