package com.codexsphere.codearena.service;

import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.entity.ExecutionLog;
import com.codexsphere.codearena.execution.context.ExecutionContext;

import java.util.Optional;

public interface ExecutionLogService {

    /**
     * Creates and persists the execution log entry for the given context.
     *
     * @throws com.codexsphere.codearena.exception.DuplicateSubmissionException
     *         when the request carries a submissionId that has already been
     *         processed.
     */
    ExecutionLog createExecutionLog(ExecutionContext context);

    /**
     * Marks the execution as completed and persists execution metrics and
     * per-test-case results.
     */
    void completeExecution(
            ExecutionContext context,
            ExecuteCodeResponse response
    );

    /**
     * Marks the execution as failed and persists the error message.
     */
    void completeExecutionWithError(
            ExecutionContext context,
            Throwable error
    );

    Optional<ExecutionLog> findBySubmissionId(Long submissionId);

    boolean existsBySubmissionId(Long submissionId);

}
