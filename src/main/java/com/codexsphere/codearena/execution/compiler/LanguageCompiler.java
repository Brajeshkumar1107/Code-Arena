package com.codexsphere.codearena.execution.compiler;

import com.codexsphere.codearena.execution.compiler.model.CompileResult;
import com.codexsphere.codearena.execution.context.ExecutionContext;

public interface LanguageCompiler {

    CompileResult compile(
            ExecutionContext context
    );

}
