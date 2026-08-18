package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.JudgeResult;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JudgeServiceImplTest {

    private TestCaseExecutor testCaseExecutor;
    private ExecutionContext context;
    private ExecuteCodeRequest request;

    private JudgeServiceImpl judgeService;

    @BeforeEach
    void setUp() {

        testCaseExecutor =
                mock(TestCaseExecutor.class);

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

        judgeService =
                new JudgeServiceImpl(
                        testCaseExecutor
                );
    }

    @Test
    void shouldReturnAcceptedWhenAllTestCasesPass() {

        TestCaseRequest testCase1 =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("10")
                        .build();

        TestCaseRequest testCase2 =
                TestCaseRequest.builder()
                        .input("10")
                        .expectedOutput("20")
                        .build();

        TestCaseRequest testCase3 =
                TestCaseRequest.builder()
                        .input("25")
                        .expectedOutput("50")
                        .build();

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        59L,
                        3L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        174L,
                        0L,
                        ""
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        165L,
                        0L,
                        ""
                );

        when(
                testCaseExecutor.execute(
                        context,
                        testCase1,
                        1
                )
        ).thenReturn(result1);

        when(
                testCaseExecutor.execute(
                        context,
                        testCase2,
                        2
                )
        ).thenReturn(result2);

        when(
                testCaseExecutor.execute(
                        context,
                        testCase3,
                        3
                )
        ).thenReturn(result3);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                Verdict.ACCEPTED,
                result.getVerdict()
        );

        assertEquals(
                3,
                result.getPassed()
        );

        assertEquals(
                3,
                result.getTotal()
        );

        assertEquals(
                398L,
                result.getExecutionTime()
        );

        assertEquals(
                3L,
                result.getMemoryUsed()
        );

        assertNull(
                result.getStdout()
        );

        assertNull(
                result.getStderr()
        );

        assertEquals(
                3,
                result.getTestCaseResults().size()
        );
    }

    @Test
    void shouldReturnWrongAnswerWhenOneTestCaseFails() {

        TestCaseRequest testCase1 =
                TestCaseRequest.builder()
                        .input("5")
                        .expectedOutput("10")
                        .build();

        TestCaseRequest testCase2 =
                TestCaseRequest.builder()
                        .input("10")
                        .expectedOutput("999")
                        .build();

        TestCaseRequest testCase3 =
                TestCaseRequest.builder()
                        .input("25")
                        .expectedOutput("50")
                        .build();

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        50L,
                        2L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.WRONG_ANSWER,
                        164L,
                        0L,
                        ""
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        152L,
                        0L,
                        ""
                );

        mockResult(
                testCase1,
                1,
                result1
        );

        mockResult(
                testCase2,
                2,
                result2
        );

        mockResult(
                testCase3,
                3,
                result3
        );

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                Verdict.WRONG_ANSWER,
                result.getVerdict()
        );

        assertEquals(
                2,
                result.getPassed()
        );

        assertEquals(
                3,
                result.getTotal()
        );

        assertEquals(
                366L,
                result.getExecutionTime()
        );

        assertEquals(
                2L,
                result.getMemoryUsed()
        );
    }

    @Test
    void shouldReturnRuntimeErrorWhenRuntimeErrorOccurs() {

        TestCaseRequest testCase1 =
                testCase(
                        "2",
                        "5.0"
                );

        TestCaseRequest testCase2 =
                testCase(
                        "0",
                        "0"
                );

        TestCaseRequest testCase3 =
                testCase(
                        "5",
                        "2.0"
                );

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        86L,
                        2L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.RUNTIME_ERROR,
                        181L,
                        0L,
                        "ZeroDivisionError"
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        149L,
                        0L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);
        mockResult(testCase3, 3, result3);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                Verdict.RUNTIME_ERROR,
                result.getVerdict()
        );

        assertEquals(
                2,
                result.getPassed()
        );

        assertEquals(
                3,
                result.getTotal()
        );

        assertEquals(
                416L,
                result.getExecutionTime()
        );

        assertEquals(
                2L,
                result.getMemoryUsed()
        );

        assertEquals(
                "ZeroDivisionError",
                result.getStderr()
        );
    }

    @Test
    void shouldReturnTimeLimitExceededWhenTimeoutOccurs() {

        TestCaseRequest testCase =
                testCase(
                        "",
                        ""
                );

        request.setTestCases(
                List.of(testCase)
        );

        TestCaseResult result =
                testCaseResult(
                        1,
                        Verdict.TIME_LIMIT_EXCEEDED,
                        2013L,
                        6L,
                        "Process execution timed out."
                );

        mockResult(
                testCase,
                1,
                result
        );

        JudgeResult judgeResult =
                judgeService.judge(context);

        assertEquals(
                Verdict.TIME_LIMIT_EXCEEDED,
                judgeResult.getVerdict()
        );

        assertEquals(
                0,
                judgeResult.getPassed()
        );

        assertEquals(
                1,
                judgeResult.getTotal()
        );

        assertEquals(
                2013L,
                judgeResult.getExecutionTime()
        );

        assertEquals(
                6L,
                judgeResult.getMemoryUsed()
        );

        assertEquals(
                "Process execution timed out.",
                judgeResult.getStderr()
        );
    }

    @Test
    void shouldReturnMemoryLimitExceededWhenMemoryLimitIsExceeded() {

        TestCaseRequest testCase =
                testCase(
                        "",
                        ""
                );

        request.setTestCases(
                List.of(testCase)
        );

        TestCaseResult result =
                testCaseResult(
                        1,
                        Verdict.MEMORY_LIMIT_EXCEEDED,
                        438L,
                        256L,
                        "Killed"
                );

        mockResult(
                testCase,
                1,
                result
        );

        JudgeResult judgeResult =
                judgeService.judge(context);

        assertEquals(
                Verdict.MEMORY_LIMIT_EXCEEDED,
                judgeResult.getVerdict()
        );

        assertEquals(
                0,
                judgeResult.getPassed()
        );

        assertEquals(
                1,
                judgeResult.getTotal()
        );

        assertEquals(
                438L,
                judgeResult.getExecutionTime()
        );

        assertEquals(
                256L,
                judgeResult.getMemoryUsed()
        );
    }

    @Test
    void shouldContinueExecutingRemainingTestCasesAfterFailure() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "999");

        TestCaseRequest testCase3 =
                testCase("3", "6");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        10L,
                        1L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.WRONG_ANSWER,
                        20L,
                        2L,
                        ""
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        30L,
                        3L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);
        mockResult(testCase3, 3, result3);

        judgeService.judge(context);

        InOrder inOrder =
                inOrder(testCaseExecutor);

        inOrder.verify(testCaseExecutor)
                .execute(context, testCase1, 1);

        inOrder.verify(testCaseExecutor)
                .execute(context, testCase2, 2);

        inOrder.verify(testCaseExecutor)
                .execute(context, testCase3, 3);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldPreserveTestCaseOrder() {

        TestCaseRequest testCase1 =
                testCase("5", "10");

        TestCaseRequest testCase2 =
                testCase("10", "20");

        TestCaseRequest testCase3 =
                testCase("25", "50");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        10L,
                        1L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        20L,
                        2L,
                        ""
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        30L,
                        3L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);
        mockResult(testCase3, 3, result3);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                result1,
                result.getTestCaseResults().get(0)
        );

        assertEquals(
                result2,
                result.getTestCaseResults().get(1)
        );

        assertEquals(
                result3,
                result.getTestCaseResults().get(2)
        );
    }

    @Test
    void shouldCalculateTotalExecutionTime() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        100L,
                        5L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        250L,
                        10L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                350L,
                result.getExecutionTime()
        );
    }

    @Test
    void shouldUseMaximumMemoryAcrossTestCases() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        TestCaseRequest testCase3 =
                testCase("3", "6");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        10L,
                        5L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        10L,
                        15L,
                        ""
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        10L,
                        8L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);
        mockResult(testCase3, 3, result3);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                15L,
                result.getMemoryUsed()
        );
    }

    @Test
    void shouldIgnoreNullExecutionTime() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2
                )
        );

        TestCaseResult result1 =
                TestCaseResult.builder()
                        .order(1)
                        .verdict(Verdict.ACCEPTED)
                        .executionTime(null)
                        .memoryUsed(5L)
                        .build();

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        100L,
                        10L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                100L,
                result.getExecutionTime()
        );
    }

    @Test
    void shouldIgnoreNullMemoryUsage() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2
                )
        );

        TestCaseResult result1 =
                TestCaseResult.builder()
                        .order(1)
                        .verdict(Verdict.ACCEPTED)
                        .executionTime(100L)
                        .memoryUsed(null)
                        .build();

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        100L,
                        10L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                10L,
                result.getMemoryUsed()
        );
    }

    @Test
    void shouldUseFirstFailedVerdictAsFinalVerdict() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        TestCaseRequest testCase3 =
                testCase("3", "6");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        10L,
                        1L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.RUNTIME_ERROR,
                        20L,
                        2L,
                        "Runtime error"
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.WRONG_ANSWER,
                        30L,
                        3L,
                        ""
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);
        mockResult(testCase3, 3, result3);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                Verdict.RUNTIME_ERROR,
                result.getVerdict()
        );
    }

    @Test
    void shouldUseFirstNonBlankErrorAsOverallStderr() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        TestCaseRequest testCase3 =
                testCase("3", "6");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.WRONG_ANSWER,
                        10L,
                        1L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.RUNTIME_ERROR,
                        20L,
                        2L,
                        "First runtime error"
                );

        TestCaseResult result3 =
                testCaseResult(
                        3,
                        Verdict.RUNTIME_ERROR,
                        30L,
                        3L,
                        "Second runtime error"
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);
        mockResult(testCase3, 3, result3);

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                "First runtime error",
                result.getStderr()
        );
    }

    @Test
    void shouldReturnNullStderrWhenNoFailedTestHasError() {

        TestCaseRequest testCase1 =
                testCase("1", "999");

        TestCaseRequest testCase2 =
                testCase("2", "999");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2
                )
        );

        TestCaseResult result1 =
                testCaseResult(
                        1,
                        Verdict.WRONG_ANSWER,
                        10L,
                        1L,
                        ""
                );

        TestCaseResult result2 =
                testCaseResult(
                        2,
                        Verdict.WRONG_ANSWER,
                        20L,
                        2L,
                        null
                );

        mockResult(testCase1, 1, result1);
        mockResult(testCase2, 2, result2);

        JudgeResult result =
                judgeService.judge(context);

        assertNull(
                result.getStderr()
        );
    }

    @Test
    void shouldReturnEmptyResultForEmptyTestCases() {

        request.setTestCases(
                List.of()
        );

        JudgeResult result =
                judgeService.judge(context);

        assertEquals(
                Verdict.ACCEPTED,
                result.getVerdict()
        );

        assertEquals(
                0,
                result.getPassed()
        );

        assertEquals(
                0,
                result.getTotal()
        );

        assertEquals(
                0L,
                result.getExecutionTime()
        );

        assertEquals(
                0L,
                result.getMemoryUsed()
        );

        assertTrue(
                result.getTestCaseResults()
                        .isEmpty()
        );

        verifyNoInteractions(
                testCaseExecutor
        );
    }

    @Test
    void shouldPassCorrectOrderToTestCaseExecutor() {

        TestCaseRequest testCase1 =
                testCase("1", "2");

        TestCaseRequest testCase2 =
                testCase("2", "4");

        TestCaseRequest testCase3 =
                testCase("3", "6");

        request.setTestCases(
                List.of(
                        testCase1,
                        testCase2,
                        testCase3
                )
        );

        when(
                testCaseExecutor.execute(
                        context,
                        testCase1,
                        1
                )
        ).thenReturn(
                testCaseResult(
                        1,
                        Verdict.ACCEPTED,
                        10L,
                        1L,
                        ""
                )
        );

        when(
                testCaseExecutor.execute(
                        context,
                        testCase2,
                        2
                )
        ).thenReturn(
                testCaseResult(
                        2,
                        Verdict.ACCEPTED,
                        20L,
                        2L,
                        ""
                )
        );

        when(
                testCaseExecutor.execute(
                        context,
                        testCase3,
                        3
                )
        ).thenReturn(
                testCaseResult(
                        3,
                        Verdict.ACCEPTED,
                        30L,
                        3L,
                        ""
                )
        );

        judgeService.judge(context);

        verify(
                testCaseExecutor
        ).execute(
                context,
                testCase1,
                1
        );

        verify(
                testCaseExecutor
        ).execute(
                context,
                testCase2,
                2
        );

        verify(
                testCaseExecutor
        ).execute(
                context,
                testCase3,
                3
        );
    }

    private void mockResult(
            TestCaseRequest testCase,
            int order,
            TestCaseResult result
    ) {

        when(
                testCaseExecutor.execute(
                        context,
                        testCase,
                        order
                )
        ).thenReturn(result);
    }

    private TestCaseRequest testCase(
            String input,
            String expectedOutput
    ) {

        return TestCaseRequest.builder()
                .input(input)
                .expectedOutput(expectedOutput)
                .build();
    }

    private TestCaseResult testCaseResult(
            int order,
            Verdict verdict,
            Long executionTime,
            Long memoryUsed,
            String stderr
    ) {

        return TestCaseResult.builder()
                .order(order)
                .verdict(verdict)
                .executionTime(executionTime)
                .memoryUsed(memoryUsed)
                .stderr(stderr)
                .build();
    }
}