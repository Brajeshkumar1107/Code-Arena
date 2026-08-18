package com.codexsphere.codearena.execution.runner;

import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.model.ExecutionResult;

public interface LanguageRunner {

    ExecutionResult run(
            ExecutionContext context
    );

}