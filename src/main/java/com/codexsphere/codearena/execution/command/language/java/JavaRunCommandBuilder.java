package com.codexsphere.codearena.execution.command.language.java;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.execution.command.CommandBuilder;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.language.LanguageMetadataFactory;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaRunCommandBuilder implements CommandBuilder {

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

        boolean docker =
                runnerProperties.getEnvironment()
                        == ExecutionEnvironmentType.DOCKER;

        Path workingDirectory =
                docker
                        ? Path.of("/workspace")
                        : context.getWorkspace().getRoot();

        /*
         * Create input.txt in the host workspace.
         *
         * The workspace is mounted into Docker as:
         *
         * /tmp/exec-xxx -> /workspace
         *
         * Therefore this file becomes:
         *
         * /workspace/input.txt
         */
        Path inputFile =
                context.getWorkspace()
                        .getRoot()
                        .resolve("input.txt");

        String input =
                context.getCurrentTestCase().getInput();

        if (input == null) {
            input = "";
        }

        if (!input.endsWith("\n")) {
            input += "\n";
        }

        try {

            Files.writeString(
                    inputFile,
                    input
            );

            log.info(
                    "Java input file created: {}",
                    inputFile
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to create Java input file: {}",
                    inputFile,
                    ex
            );

            throw new RuntimeException(
                    "Unable to create Java input file.",
                    ex
            );
        }

        /*
         * Docker:
         *
         * java Main < /workspace/input.txt
         *
         * Local:
         *
         * java Main
         */
        List<String> command;

        if (docker) {

            command = List.of(
                    "sh",
                    "-c",
                    "java "
                            + metadata.getExecutableName()
                            + " < /workspace/input.txt"
            );

        } else {

            command = List.of(
                    "java",
                    metadata.getExecutableName()
            );
        }

        log.info(
                "Java execution command: {}",
                String.join(" ", command)
        );

        return ProcessRequest.builder()
                .command(command)
                .workingDirectory(workingDirectory)
                .timeout(
                        Duration.ofSeconds(
                                runnerProperties
                                        .getExecution()
                                        .getTimeout()
                        )
                )
                /*
                 * Local execution:
                 * ProcessBuilder receives stdin directly.
                 *
                 * Docker execution:
                 * stdin is provided through input.txt redirection.
                 */
                .input(
                        docker
                                ? null
                                : new ByteArrayInputStream(
                                input.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                )
                .inputFile(inputFile)
                .containerId(
                        context.getDockerContainer() == null
                                ? null
                                : context.getDockerContainer().getId()
                )
                .build();
    }
}