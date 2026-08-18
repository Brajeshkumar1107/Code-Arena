package com.codexsphere.codearena.execution.runner.language;

import com.codexsphere.codearena.execution.command.language.java.JavaRunCommandBuilder;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import com.codexsphere.codearena.execution.process.ProcessExecutor;
import com.codexsphere.codearena.execution.process.ProcessExecutorFactory;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;
import com.codexsphere.codearena.execution.runner.LanguageRunner;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JavaLanguageRunner implements LanguageRunner {

    private final ProcessExecutorFactory processExecutorFactory;
    private final JavaRunCommandBuilder commandBuilder;

    @Override
    public ExecutionResult run(
            ExecutionContext context
    ) {

        ProcessRequest request =
                commandBuilder.build(context);

        ProcessExecutor executor =
                processExecutorFactory.getExecutor();

        ProcessResult result =
                executor.execute(request);

        return ExecutionResult.builder()
                .success(
                        !result.isTimeout()
                                && !result.isMemoryLimitExceeded()
                                && !result.isOutputLimitExceeded()
                                && result.getExitCode() == 0
                )
                .timeout(result.isTimeout())
                .stdout(result.getStdout())
                .stderr(result.getStderr())
                .memoryUsedMb(
                        result.getMemoryUsedMb()
                )
                .exitCode(result.getExitCode())
                .executionTimeMillis(
                        result.getExecutionTimeMillis()
                )
                .memoryLimitExceeded(
                        result.isMemoryLimitExceeded()
                )
                .outputLimitExceeded(
                        result.isOutputLimitExceeded()
                )
                .build();
    }
}
