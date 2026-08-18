package com.codexsphere.codearena.execution.compiler.language;

import com.codexsphere.codearena.execution.command.language.cpp.CppCompileCommandBuilder;
import com.codexsphere.codearena.execution.command.language.java.JavaCompileCommandBuilder;
import com.codexsphere.codearena.execution.compiler.LanguageCompiler;
import com.codexsphere.codearena.execution.compiler.model.CompileResult;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.process.ProcessExecutor;
import com.codexsphere.codearena.execution.process.ProcessExecutorFactory;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CppLanguageCompiler implements LanguageCompiler {

    private final ProcessExecutorFactory processExecutorFactory;

    private final CppCompileCommandBuilder commandBuilder;

    @Override
    public CompileResult compile(
            ExecutionContext context
    ) {

        log.info("========== CPP LANGUAGE COMPILER CALLED ==========");
        ProcessRequest request =
                commandBuilder.build(context);

        ProcessExecutor executor =
                processExecutorFactory.getExecutor();

        ProcessResult result =
                executor.execute(request);

        return CompileResult.builder()
                .success(result.getExitCode() == 0)
                .stdout(result.getStdout())
                .stderr(result.getStderr())
                .exitCode(result.getExitCode())
                .build();
    }
}
