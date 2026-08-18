package com.codexsphere.codearena.execution.compiler.language;

import com.codexsphere.codearena.execution.compiler.LanguageCompiler;
import com.codexsphere.codearena.execution.compiler.model.CompileResult;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PythonLanguageCompiler implements LanguageCompiler {

    @Override
    public CompileResult compile(
            ExecutionContext context
    ) {

        log.info(
                "Python compilation skipped - interpreted language"
        );

        return CompileResult.builder()
                .success(true)
                .stdout("")
                .stderr("")
                .exitCode(0)
                .build();
    }
}