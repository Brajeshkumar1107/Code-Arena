package com.codexsphere.codearena.execution.command.language.java;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.execution.command.CommandBuilder;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.language.LanguageMetadataFactory;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaCompileCommandBuilder
        implements CommandBuilder {

    private final LanguageMetadataFactory metadataFactory;
    private final RunnerProperties runnerProperties;

    @Override
    public ProcessRequest build(
            ExecutionContext context
    ) {

        LanguageMetadata metadata =
                metadataFactory.get(
                        context.getRequest().getLanguage()
                );

        Path sourceFile =
                context.getWorkspace()
                        .getRoot()
                        .resolve(metadata.getSourceFileName());

        Path workingDirectory =
                runnerProperties.getEnvironment() == ExecutionEnvironmentType.DOCKER
                        ? Path.of("/workspace")
                        : context.getWorkspace().getRoot();

        return ProcessRequest.builder()
                .command(
                        List.of(
                                "javac",
                                sourceFile.getFileName().toString()
                        )
                )
                .workingDirectory(workingDirectory)
                .timeout(
                        Duration.ofSeconds(
                                runnerProperties
                                        .getCompile()
                                        .getTimeout()
                        )
                )
                .containerId(
                        context.getDockerContainer() == null
                                ? null
                                : context.getDockerContainer().getId()
                )
                .build();
    }

}
