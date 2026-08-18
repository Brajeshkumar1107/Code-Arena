package com.codexsphere.codearena.execution.compiler;

import com.codexsphere.codearena.enums.Language;

public interface LanguageCompilerFactory {

    LanguageCompiler getCompiler(
            Language language
    );

}
