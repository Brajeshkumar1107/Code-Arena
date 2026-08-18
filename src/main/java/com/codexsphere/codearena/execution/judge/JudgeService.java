package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.JudgeResult;

public interface JudgeService {

    /**
     * Compiles (if required), executes all test cases,
     * compares outputs, and returns the final verdict.
     *
     * @param context Execution context containing submission,
     *                workspace, and execution details.
     * @return Final judging result.
     */
    JudgeResult judge(ExecutionContext context);

}
