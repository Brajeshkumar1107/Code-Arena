package com.codexsphere.codearena.execution.source.strategy;

import com.codexsphere.codearena.exception.SourceFileException;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.language.LanguageMetadataFactory;
import com.codexsphere.codearena.execution.source.SourceFileWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class CppSourceFileWriter implements SourceFileWriter {

    private final LanguageMetadataFactory metadataFactory;


    @Override
    public void writeSourceFile(ExecutionContext context) {

        try {

            LanguageMetadata metadata =
                    metadataFactory.get(
                            context.getRequest().getLanguage()
                    );

            Path sourceFile = context.getWorkspace()
                    .getRoot()
                    .resolve(metadata.getSourceFileName());

            Files.writeString(
                    sourceFile,
                    context.getRequest().getSourceCode()
            );

            context.getWorkspace().setSourceFile(sourceFile);

            log.info("Source file created successfully: {}", sourceFile);

        } catch (IOException ex) {

            log.error("Failed to write source file.", ex);

            throw new SourceFileException(
                    "Unable to write source file.",
                    ex
            );
        }
    }
}
