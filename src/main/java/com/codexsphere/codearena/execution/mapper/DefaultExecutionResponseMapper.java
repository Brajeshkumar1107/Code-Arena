package com.codexsphere.codearena.execution.mapper;

import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.compiler.model.CompileResult;
import com.codexsphere.codearena.execution.judge.model.JudgeResult;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DefaultExecutionResponseMapper
        implements ExecutionResponseMapper {

    @Override
    public ExecuteCodeResponse fromCompileResult(
            CompileResult result
    ) {

        return ExecuteCodeResponse.builder()
                .success(false)
                .verdict(Verdict.COMPILATION_ERROR)
                .stdout(
                        result.getStdout() == null
                                ? ""
                                : result.getStdout()
                )
                .stderr(
                        result.getStderr() == null
                                ? ""
                                : result.getStderr()
                )
                .passed(0)
                .total(0)
                .executionTime(0L)
                .memoryUsed(0L)
                .testCases(Collections.emptyList())
                .build();
    }

    @Override
    public ExecuteCodeResponse fromExecutionResult(
            ExecutionResult result
    ) {

        Verdict verdict;

        if (result.isTimeout()) {

            verdict = Verdict.TIME_LIMIT_EXCEEDED;

        } else if (result.isMemoryLimitExceeded()) {

            verdict = Verdict.MEMORY_LIMIT_EXCEEDED;

        } else if (result.isOutputLimitExceeded()) {

            verdict = Verdict.OUTPUT_LIMIT_EXCEEDED;

        } else if (!result.isSuccess()) {

            verdict = Verdict.RUNTIME_ERROR;

        } else {

            verdict = Verdict.ACCEPTED;
        }

        return ExecuteCodeResponse.builder()
                .success(result.isSuccess())
                .verdict(verdict)
                .stdout(
                        result.getStdout() == null
                                ? ""
                                : result.getStdout()
                )
                .stderr(
                        result.getStderr() == null
                                ? ""
                                : result.getStderr()
                )
                .passed(0)
                .total(0)
                .executionTime(
                        result.getExecutionTimeMillis() == null
                                ? 0L
                                : result.getExecutionTimeMillis()
                )
                .memoryUsed(
                        result.getMemoryUsedMb() == null
                                ? 0L
                                : result.getMemoryUsedMb()
                )
                .testCases(Collections.emptyList())
                .build();
    }

    @Override
    public ExecuteCodeResponse fromJudgeResult(
            JudgeResult result
    ) {

        return ExecuteCodeResponse.builder()
                .success(
                        result.getVerdict()
                                == Verdict.ACCEPTED
                )
                .verdict(result.getVerdict())
                .stdout(
                        result.getStdout() == null
                                ? ""
                                : result.getStdout()
                )
                .stderr(
                        result.getStderr() == null
                                ? ""
                                : result.getStderr()
                )
                .passed(
                        result.getPassed() == null
                                ? 0
                                : result.getPassed()
                )
                .total(
                        result.getTotal() == null
                                ? 0
                                : result.getTotal()
                )
                .executionTime(
                        result.getExecutionTime() == null
                                ? 0L
                                : result.getExecutionTime()
                )
                .memoryUsed(
                        result.getMemoryUsed() == null
                                ? 0L
                                : result.getMemoryUsed()
                )
                .testCases(
                        result.getTestCaseResults() == null
                                ? Collections.emptyList()
                                : result.getTestCaseResults()
                )
                .build();
    }
}