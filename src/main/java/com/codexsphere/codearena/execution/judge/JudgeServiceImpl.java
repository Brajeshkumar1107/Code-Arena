package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.JudgeResult;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final TestCaseExecutor testCaseExecutor;

    @Override
    public JudgeResult judge(ExecutionContext context) {

        List<TestCaseRequest> testCases =
                context.getRequest().getTestCases();

        List<TestCaseResult> results =
                new ArrayList<>();

        long totalExecutionTime = 0L;
        long maxMemory = 0L;

        for (int i = 0; i < testCases.size(); i++) {

            TestCaseRequest testCase =
                    testCases.get(i);

            TestCaseResult result =
                    testCaseExecutor.execute(
                            context,
                            testCase,
                            i + 1
                    );

            results.add(result);

            if (result.getExecutionTime() != null) {
                totalExecutionTime +=
                        result.getExecutionTime();
            }

            if (result.getMemoryUsed() != null) {
                maxMemory = Math.max(
                        maxMemory,
                        result.getMemoryUsed()
                );
            }
        }

        long passed =
                results.stream()
                        .filter(
                                r -> r.getVerdict()
                                        == Verdict.ACCEPTED
                        )
                        .count();

        Verdict finalVerdict =
                passed == results.size()
                        ? Verdict.ACCEPTED
                        : results.stream()
                        .filter(
                                r -> r.getVerdict()
                                        != Verdict.ACCEPTED
                        )
                        .findFirst()
                        .map(TestCaseResult::getVerdict)
                        .orElse(Verdict.WRONG_ANSWER);

        /*
         * Find the first failed test case.
         *
         * Its stderr represents the most useful
         * execution error for the overall response.
         */
        String stderr =
                results.stream()
                        .filter(
                                r -> r.getVerdict()
                                        != Verdict.ACCEPTED
                        )
                        .map(TestCaseResult::getStderr)
                        .filter(
                                error -> error != null
                                        && !error.isBlank()
                        )
                        .findFirst()
                        .orElse(null);

        /*
         * Overall stdout is not particularly useful
         * in JUDGE mode because every test case has
         * its own actual output.
         */
        String stdout = null;

        return JudgeResult.builder()
                .verdict(finalVerdict)
                .passed((int) passed)
                .total(results.size())
                .executionTime(totalExecutionTime)
                .memoryUsed(maxMemory)
                .stdout(stdout)
                .stderr(stderr)
                .testCaseResults(results)
                .build();
    }
}