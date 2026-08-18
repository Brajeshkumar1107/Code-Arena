package com.codexsphere.codearena.service.impl;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.entity.ExecutionLog;
import com.codexsphere.codearena.entity.ExecutionMetric;
import com.codexsphere.codearena.entity.ExecutionTestCaseResult;
import com.codexsphere.codearena.enums.ExecutionStatus;
import com.codexsphere.codearena.exception.DuplicateSubmissionException;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;
import com.codexsphere.codearena.repository.ExecutionLogRepository;
import com.codexsphere.codearena.repository.ExecutionMetricRepository;
import com.codexsphere.codearena.repository.ExecutionTestCaseResultRepository;
import com.codexsphere.codearena.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionLogServiceImpl
        implements ExecutionLogService {

    private final ExecutionLogRepository repository;
    private final ExecutionMetricRepository metricRepository;
    private final ExecutionTestCaseResultRepository testCaseResultRepository;

    @Override
    @Transactional
    public ExecutionLog createExecutionLog(
            ExecutionContext context
    ) {

        ExecuteCodeRequest request =
                context.getRequest();

        Long submissionId =
                request.getSubmissionId();

        if (submissionId != null
                && repository.existsBySubmissionId(
                submissionId
        )) {

            throw new DuplicateSubmissionException(
                    submissionId
            );
        }

        String dockerImage =
                context.getDockerContainer() == null
                        ? null
                        : context.getDockerContainer()
                        .getImage();

        ExecutionLog executionLog =
                ExecutionLog.builder()
                        .submissionId(submissionId)
                        .problemId(request.getProblemId())
                        .language(request.getLanguage())
                        .status(ExecutionStatus.RUNNING)
                        .containerId(
                                context.getDockerContainer() == null
                                        ? null
                                        : context.getDockerContainer()
                                        .getId()
                        )
                        .dockerImage(dockerImage)
                        .startedAt(LocalDateTime.now())
                        .build();

        ExecutionLog saved =
                repository.save(executionLog);

        context.setExecutionLog(saved);

        log.debug(
                "[{}] Execution log created. logId={}",
                context.getExecutionId(),
                saved.getId()
        );

        return saved;
    }

    @Override
    @Transactional
    public void completeExecution(
            ExecutionContext context,
            ExecuteCodeResponse response
    ) {

        ExecutionLog executionLog =
                context.getExecutionLog();

        if (executionLog == null) {

            return;
        }

        executionLog.setStatus(
                response.isSuccess()
                        ? ExecutionStatus.SUCCESS
                        : ExecutionStatus.COMPLETED
        );

        executionLog.setCompletedAt(
                LocalDateTime.now()
        );

        executionLog.setExitCode(
                response.isSuccess() ? 0 : -1
        );

        if (!response.isSuccess()
                && response.getVerdict() != null) {

            executionLog.setErrorMessage(
                    response.getVerdict().name()
            );
        }

        repository.save(executionLog);

        persistMetric(
                executionLog,
                response
        );

        persistTestCaseResults(
                executionLog,
                response.getTestCases()
        );
    }

    @Override
    @Transactional
    public void completeExecutionWithError(
            ExecutionContext context,
            Throwable error
    ) {

        ExecutionLog executionLog =
                context.getExecutionLog();

        if (executionLog == null) {

            return;
        }

        executionLog.setStatus(ExecutionStatus.FAILED);
        executionLog.setCompletedAt(LocalDateTime.now());
        executionLog.setExitCode(-1);

        if (error != null) {

            String message =
                    error.getMessage();

            executionLog.setErrorMessage(
                    message != null
                            && message.length() > 3000
                            ? message.substring(0, 3000)
                            : message
            );
        }

        repository.save(executionLog);
    }

    private void persistMetric(
            ExecutionLog executionLog,
            ExecuteCodeResponse response
    ) {

        ExecutionMetric metric =
                ExecutionMetric.builder()
                        .executionLog(executionLog)
                        .executionTime(response.getExecutionTime())
                        .memoryUsed(response.getMemoryUsed())
                        .verdict(response.getVerdict())
                        .totalTestCases(response.getTotal())
                        .passedTestCases(response.getPassed())
                        .build();

        metricRepository.save(metric);
    }

    private void persistTestCaseResults(
            ExecutionLog executionLog,
            List<TestCaseResult> testCases
    ) {

        if (testCases == null || testCases.isEmpty()) {

            return;
        }

        for (TestCaseResult testCase : testCases) {

            if (testCase.getVerdict() == null) {

                continue;
            }

            ExecutionTestCaseResult entity =
                    ExecutionTestCaseResult.builder()
                            .executionLog(executionLog)
                            .orderNo(testCase.getOrder())
                            .verdict(testCase.getVerdict())
                            .executionTime(testCase.getExecutionTime())
                            .memoryUsed(testCase.getMemoryUsed())
                            .input(testCase.getInput())
                            .expected(testCase.getExpected())
                            .actual(testCase.getActual())
                            .stderr(testCase.getStderr())
                            .build();

            testCaseResultRepository.save(entity);
        }
    }

    @Override
    public Optional<ExecutionLog> findBySubmissionId(
            Long submissionId
    ) {

        return repository.findBySubmissionId(submissionId);
    }

    @Override
    public boolean existsBySubmissionId(
            Long submissionId
    ) {

        return repository.existsBySubmissionId(submissionId);
    }
}
