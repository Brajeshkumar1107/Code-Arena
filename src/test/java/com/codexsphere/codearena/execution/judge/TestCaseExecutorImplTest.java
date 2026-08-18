package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import com.codexsphere.codearena.execution.runner.LanguageRunner;
import com.codexsphere.codearena.execution.runner.LanguageRunnerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestCaseExecutorImplTest {

    private LanguageRunnerFactory runnerFactory;
    private VerdictComparator comparator;
    private LanguageRunner languageRunner;
    private ExecutionContext context;
    private ExecuteCodeRequest request;

    private TestCaseExecutorImpl executor;

    @BeforeEach
    void setUp() {

        runnerFactory =
                mock(LanguageRunnerFactory.class);

        comparator =
                mock(VerdictComparator.class);

        languageRunner =
                mock(LanguageRunner.class);

        context =
                mock(ExecutionContext.class);

        request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.JUDGE)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .build();

        when(context.getRequest())
                .thenReturn(request);

        when(
                runnerFactory.getRunner(
                        Language.PYTHON
                )
        ).thenReturn(languageRunner);

        executor =
                new TestCaseExecutorImpl(
                        runnerFactory,
                        comparator
                );
    }

    @Test
    void shouldReturnAcceptedWhenExecutionSucceedsAndOutputMatches() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("10")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(true)
                        .exitCode(0)
                        .stdout("10\n")
                        .stderr("")
                        .timeout(false)
                        .memoryLimitExceeded(false)
                        .outputLimitExceeded(false)
                        .executionTimeMillis(25L)
                        .memoryUsedMb(4L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        when(
                comparator.compare(
                        testCase,
                        executionResult
                )
        ).thenReturn(Verdict.ACCEPTED);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.ACCEPTED,
                result.getVerdict()
        );

        assertEquals(
                1,
                result.getOrder()
        );

        assertEquals(
                "5",
                result.getInput()
        );

        assertEquals(
                "10",
                result.getExpected()
        );

        assertEquals(
                "10\n",
                result.getActual()
        );

        assertEquals(
                "",
                result.getStderr()
        );

        assertEquals(
                25L,
                result.getExecutionTime()
        );

        assertEquals(
                4L,
                result.getMemoryUsed()
        );

        verify(comparator)
                .compare(
                        testCase,
                        executionResult
                );
    }

    @Test
    void shouldReturnWrongAnswerWhenOutputDoesNotMatch() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("20")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(true)
                        .exitCode(0)
                        .stdout("10\n")
                        .stderr("")
                        .executionTimeMillis(20L)
                        .memoryUsedMb(3L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        when(
                comparator.compare(
                        testCase,
                        executionResult
                )
        ).thenReturn(Verdict.WRONG_ANSWER);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.WRONG_ANSWER,
                result.getVerdict()
        );

        verify(comparator)
                .compare(
                        testCase,
                        executionResult
                );
    }

    @Test
    void shouldReturnTimeLimitExceededWhenExecutionTimesOut() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(null)
                        .stdout("")
                        .stderr("Process execution timed out.")
                        .timeout(true)
                        .executionTimeMillis(2000L)
                        .memoryUsedMb(6L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.TIME_LIMIT_EXCEEDED,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldReturnMemoryLimitExceededWhenMemoryLimitIsExceeded() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(null)
                        .stdout("")
                        .stderr("Killed")
                        .memoryLimitExceeded(true)
                        .executionTimeMillis(438L)
                        .memoryUsedMb(256L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.MEMORY_LIMIT_EXCEEDED,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldReturnOutputLimitExceededWhenOutputLimitIsExceeded() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(0)
                        .stdout("very large output")
                        .stderr("")
                        .outputLimitExceeded(true)
                        .executionTimeMillis(100L)
                        .memoryUsedMb(5L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.OUTPUT_LIMIT_EXCEEDED,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldReturnRuntimeErrorWhenProcessExitCodeIsNonZero() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(1)
                        .stdout("")
                        .stderr("Runtime error")
                        .executionTimeMillis(50L)
                        .memoryUsedMb(2L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.RUNTIME_ERROR,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldSetCurrentTestCaseBeforeExecution() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("10")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(true)
                        .exitCode(0)
                        .stdout("10\n")
                        .stderr("")
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        when(
                comparator.compare(
                        testCase,
                        executionResult
                )
        ).thenReturn(Verdict.ACCEPTED);

        executor.execute(
                context,
                testCase,
                1
        );

        verify(context)
                .setCurrentTestCase(testCase);

        verify(languageRunner)
                .run(context);
    }

    @Test
    void shouldUseLanguageFromExecutionContext() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("10")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(true)
                        .exitCode(0)
                        .stdout("10")
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        when(
                comparator.compare(
                        testCase,
                        executionResult
                )
        ).thenReturn(Verdict.ACCEPTED);

        executor.execute(
                context,
                testCase,
                1
        );

        verify(runnerFactory)
                .getRunner(Language.PYTHON);
    }

    @Test
    void shouldPreserveTestCaseOrder() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("25")
                        .expectedOutput("50")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(true)
                        .exitCode(0)
                        .stdout("50\n")
                        .stderr("")
                        .executionTimeMillis(100L)
                        .memoryUsedMb(5L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        when(
                comparator.compare(
                        testCase,
                        executionResult
                )
        ).thenReturn(Verdict.ACCEPTED);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        7
                );

        assertEquals(
                7,
                result.getOrder()
        );
    }

    @Test
    void shouldPropagateExecutionOutputAndError() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("10")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(1)
                        .stdout("partial output")
                        .stderr("Traceback...")
                        .executionTimeMillis(80L)
                        .memoryUsedMb(7L)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                "partial output",
                result.getActual()
        );

        assertEquals(
                "Traceback...",
                result.getStderr()
        );

        assertEquals(
                80L,
                result.getExecutionTime()
        );

        assertEquals(
                7L,
                result.getMemoryUsed()
        );

        assertEquals(
                Verdict.RUNTIME_ERROR,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldGiveTimeoutPriorityOverMemoryLimit() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(null)
                        .timeout(true)
                        .memoryLimitExceeded(true)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.TIME_LIMIT_EXCEEDED,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldGiveMemoryLimitPriorityOverOutputLimit() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(null)
                        .memoryLimitExceeded(true)
                        .outputLimitExceeded(true)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.MEMORY_LIMIT_EXCEEDED,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldGiveOutputLimitPriorityOverRuntimeError() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("")
                        .expectedOutput("")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(1)
                        .outputLimitExceeded(true)
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        TestCaseResult result =
                executor.execute(
                        context,
                        testCase,
                        1
                );

        assertEquals(
                Verdict.OUTPUT_LIMIT_EXCEEDED,
                result.getVerdict()
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldNotInvokeComparatorForRuntimeError() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("0")
                        .expectedOutput("5")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(false)
                        .exitCode(1)
                        .stderr("ZeroDivisionError")
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        executor.execute(
                context,
                testCase,
                1
        );

        verifyNoInteractions(comparator);
    }

    @Test
    void shouldInvokeComparatorOnlyForSuccessfulProcess() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("10")
                        .build();

        ExecutionResult executionResult =
                ExecutionResult.builder()
                        .success(true)
                        .exitCode(0)
                        .stdout("10\n")
                        .build();

        when(languageRunner.run(context))
                .thenReturn(executionResult);

        when(
                comparator.compare(
                        testCase,
                        executionResult
                )
        ).thenReturn(Verdict.ACCEPTED);

        executor.execute(
                context,
                testCase,
                1
        );

        verify(comparator, times(1))
                .compare(
                        testCase,
                        executionResult
                );
    }
}