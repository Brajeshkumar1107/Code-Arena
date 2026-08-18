package com.codexsphere.codearena.execution.command.language.cpp;

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
public class CppRunCommandBuilder implements CommandBuilder {

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
         * Host:
         * /tmp/exec-xxx/input.txt
         *
         * Docker:
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
                    "C++ input file created: {}",
                    inputFile
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to create C++ input file: {}",
                    inputFile,
                    ex
            );

            throw new RuntimeException(
                    "Unable to create C++ input file.",
                    ex
            );
        }

        /*
         * Docker:
         *
         * ./main < /workspace/input.txt
         *
         * Local:
         *
         * ./main
         */
        List<String> command;

        if (docker) {

            command = List.of(
                    "sh",
                    "-c",
                    "./"
                            + metadata.getExecutableName()
                            + " < /workspace/input.txt"
            );

        } else {

            command = List.of(
                    "./" + metadata.getExecutableName()
            );
        }

        log.info(
                "C++ execution command: {}",
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