package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.entity.TestCase;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.model.ExecutionResult;

public interface VerdictComparator {

    Verdict compare(
            TestCaseRequest testCase,
            ExecutionResult result
    );

}
