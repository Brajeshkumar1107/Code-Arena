package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.entity.TestCase;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;

public interface TestCaseExecutor {

    TestCaseResult execute(
            ExecutionContext context,
            TestCaseRequest testCase,
            int order
    );

}