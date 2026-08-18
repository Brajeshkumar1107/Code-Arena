package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import com.codexsphere.codearena.execution.runner.LanguageRunner;
import com.codexsphere.codearena.execution.runner.LanguageRunnerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestCaseExecutorImpl implements TestCaseExecutor {

    private final LanguageRunnerFactory runnerFactory;
    private final VerdictComparator comparator;

    @Override
    public TestCaseResult execute(
            ExecutionContext context,
            TestCaseRequest testCase,
            int order
    ) {

        context.setCurrentTestCase(testCase);

        LanguageRunner languageRunner =
                runnerFactory.getRunner(
                        context.getRequest().getLanguage()
                );

        ExecutionResult executionResult =
                languageRunner.run(context);

        Verdict verdict =
                determineVerdict(
                        testCase,
                        executionResult
                );

        return TestCaseResult.builder()
                .order(order)
                .input(testCase.getInput())
                .expected(testCase.getExpectedOutput())
                .actual(executionResult.getStdout())
                .stderr(executionResult.getStderr())
                .verdict(verdict)
                .executionTime(
                        executionResult.getExecutionTimeMillis()
                )
                .memoryUsed(
                        executionResult.getMemoryUsedMb()
                )
                .build();
    }

    private Verdict determineVerdict(
            TestCaseRequest testCase,
            ExecutionResult executionResult
    ) {

        if (executionResult.isTimeout()) {
            return Verdict.TIME_LIMIT_EXCEEDED;
        }

        if (executionResult.isMemoryLimitExceeded()) {
            return Verdict.MEMORY_LIMIT_EXCEEDED;
        }

        if (executionResult.isOutputLimitExceeded()) {
            return Verdict.OUTPUT_LIMIT_EXCEEDED;
        }

        if (executionResult.getExitCode() != 0) {
            return Verdict.RUNTIME_ERROR;
        }

        return comparator.compare(
                testCase,
                executionResult
        );
    }
}