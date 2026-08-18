package com.codexsphere.codearena.execution.runner.impl;

import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.execution.runner.LanguageRunner;
import com.codexsphere.codearena.execution.runner.LanguageRunnerFactory;
import com.codexsphere.codearena.execution.runner.language.CppLanguageRunner;
import com.codexsphere.codearena.execution.runner.language.JavaLanguageRunner;
import com.codexsphere.codearena.execution.runner.language.JavaScriptLanguageRunner;
import com.codexsphere.codearena.execution.runner.language.PythonLanguageRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultLanguageRunnerFactory
        implements LanguageRunnerFactory {

    private final JavaLanguageRunner javaRunner;
    private final CppLanguageRunner cppRunner;
    private final PythonLanguageRunner pythonRunner;
    private final JavaScriptLanguageRunner javaScriptRunner;

    @Override
    public LanguageRunner getRunner(Language language) {

        return switch (language) {

            case JAVA -> javaRunner;

            case PYTHON -> pythonRunner;

            case CPP -> cppRunner;

            case JAVASCRIPT -> javaScriptRunner;

            default ->
                    throw new IllegalArgumentException(
                            "Language not supported : " + language
                    );

        };

    }

}