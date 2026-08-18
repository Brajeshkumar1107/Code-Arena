package com.codexsphere.codearena.execution.language;

import com.codexsphere.codearena.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LanguageMetadata {

    private final Language language;

    private final String sourceFileName;

    private final String executableName;

    private final String dockerImage;

    private final String workspacePath;

}