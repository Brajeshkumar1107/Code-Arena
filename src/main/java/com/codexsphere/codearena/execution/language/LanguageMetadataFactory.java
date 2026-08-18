package com.codexsphere.codearena.execution.language;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LanguageMetadataFactory {

    private final RunnerProperties runnerProperties;

    public LanguageMetadata get(Language language) {

        RunnerProperties.Languages images =
                runnerProperties.getLanguages();

        return switch (language) {

            case JAVA -> LanguageMetadata.builder()
                    .language(Language.JAVA)
                    .sourceFileName("Main.java")
                    .executableName("Main")
                    .dockerImage(images.getJavaImage())
                    .workspacePath("/workspace")
                    .build();

            case CPP -> LanguageMetadata.builder()
                    .language(Language.CPP)
                    .sourceFileName("main.cpp")
                    .executableName("main")
                    .dockerImage(images.getCppImage())
                    .workspacePath("/workspace")
                    .build();

            case PYTHON -> LanguageMetadata.builder()
                    .language(Language.PYTHON)
                    .sourceFileName("main.py")
                    .executableName(null)
                    .dockerImage(images.getPythonImage())
                    .workspacePath("/workspace")
                    .build();

            case JAVASCRIPT -> LanguageMetadata.builder()
                    .language(Language.JAVASCRIPT)
                    .sourceFileName("main.js")
                    .executableName(null)
                    .dockerImage(images.getJavascriptImage())
                    .workspacePath("/workspace")
                    .build();
        };
    }
}
