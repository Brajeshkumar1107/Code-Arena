package com.codexsphere.codearena.execution.runner;

import com.codexsphere.codearena.enums.Language;

public interface LanguageRunnerFactory {

    LanguageRunner getRunner(
            Language language
    );

}
