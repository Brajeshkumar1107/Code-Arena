package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import org.springframework.stereotype.Component;

@Component
public class DefaultVerdictComparator
        implements VerdictComparator {

    @Override
    public Verdict compare(
            TestCaseRequest testCase,
            ExecutionResult result
    ) {

        String expected =
                testCase.getExpectedOutput() == null
                        ? ""
                        : testCase.getExpectedOutput().trim();

        String actual =
                result.getStdout() == null
                        ? ""
                        : result.getStdout().trim();

        /*
         * Empty expected output means no assertion on the
         * program's stdout — only that it ran successfully.
         */
        if (expected.isBlank()) {

            return Verdict.ACCEPTED;
        }

        if (expected.equals(actual)) {

            return Verdict.ACCEPTED;
        }

        return Verdict.WRONG_ANSWER;
    }
}