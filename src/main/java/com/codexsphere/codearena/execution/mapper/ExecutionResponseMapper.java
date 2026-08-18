package com.codexsphere.codearena.execution.mapper;

import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.execution.compiler.model.CompileResult;
import com.codexsphere.codearena.execution.judge.model.JudgeResult;
import com.codexsphere.codearena.execution.model.ExecutionResult;

public interface ExecutionResponseMapper {

    ExecuteCodeResponse fromCompileResult(
            CompileResult compileResult
    );

    ExecuteCodeResponse fromExecutionResult(
            ExecutionResult executionResult
    );

    ExecuteCodeResponse fromJudgeResult(
            JudgeResult judgeResult
    );

}
