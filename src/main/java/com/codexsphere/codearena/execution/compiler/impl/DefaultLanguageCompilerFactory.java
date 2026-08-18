package com.codexsphere.codearena.execution.compiler.impl;

import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.execution.compiler.LanguageCompiler;
import com.codexsphere.codearena.execution.compiler.LanguageCompilerFactory;
import com.codexsphere.codearena.execution.compiler.language.CppLanguageCompiler;
import com.codexsphere.codearena.execution.compiler.language.JavaLanguageCompiler;
import com.codexsphere.codearena.execution.compiler.language.JavaScriptLanguageCompiler;
import com.codexsphere.codearena.execution.compiler.language.PythonLanguageCompiler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultLanguageCompilerFactory
        implements LanguageCompilerFactory {

    private final JavaLanguageCompiler javaCompiler;
    private final CppLanguageCompiler cppCompiler;
    private final PythonLanguageCompiler pythonCompiler;
    private final JavaScriptLanguageCompiler javaScriptCompiler;


    @Override
    public LanguageCompiler getCompiler(
            Language language
    ) {

        return switch (language) {

            case JAVA -> javaCompiler;

            case CPP -> cppCompiler;

            case PYTHON -> pythonCompiler;

            case JAVASCRIPT -> javaScriptCompiler;

        };

    }

}