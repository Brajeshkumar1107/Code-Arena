package com.codexsphere.codearena.execution;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.exception.DuplicateSubmissionException;
import com.codexsphere.codearena.execution.compiler.LanguageCompiler;
import com.codexsphere.codearena.execution.compiler.LanguageCompilerFactory;
import com.codexsphere.codearena.execution.compiler.model.CompileResult;
import com.codexsphere.codearena.execution.concurrency.ExecutionConcurrencyLimiter;
import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.context.ExecutionContextFactory;
import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.docker.service.DockerService;
import com.codexsphere.codearena.execution.judge.JudgeService;
import com.codexsphere.codearena.execution.judge.model.JudgeResult;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.language.LanguageMetadataFactory;
import com.codexsphere.codearena.execution.mapper.ExecutionResponseMapper;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import com.codexsphere.codearena.execution.runner.LanguageRunner;
import com.codexsphere.codearena.execution.runner.LanguageRunnerFactory;
import com.codexsphere.codearena.execution.source.SourceFileWriter;
import com.codexsphere.codearena.execution.source.SourceFileWriterFactory;
import com.codexsphere.codearena.execution.validation.ExecutionRequestValidator;
import com.codexsphere.codearena.execution.workspace.WorkspaceService;
import com.codexsphere.codearena.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionManager {

    private final WorkspaceService workspaceService;
    private final SourceFileWriterFactory sourceFileWriterFactory;
    private final ExecutionContextFactory executionContextFactory;
    private final ExecutionResponseMapper responseMapper;
    private final LanguageCompilerFactory compilerFactory;
    private final LanguageRunnerFactory runnerFactory;
    private final JudgeService judgeService;
    private final RunnerProperties runnerProperties;
    private final DockerService dockerService;
    private final LanguageMetadataFactory metadataFactory;
    private final ExecutionConcurrencyLimiter concurrencyLimiter;
    private final ExecutionRequestValidator requestValidator;
    private final ExecutionLogService executionLogService;

    public ExecuteCodeResponse execute(
            ExecuteCodeRequest request
    ) {

        /*
         * Validate request before consuming an execution slot.
         */
        requestValidator.validate(request);

        /*
         * Acquire execution slot.
         *
         * This protects the runner from handling more
         * executions than the configured limit.
         */
        log.debug(
                "Request validated. Acquiring execution slot. mode={}, language={}",
                request.getMode(),
                request.getLanguage()
        );

        concurrencyLimiter.acquire();

        ExecutionContext context = null;

        ExecuteCodeResponse response = null;

        Throwable error = null;

        try {

            /*
             * Create execution context after acquiring
             * the concurrency slot.
             */
            context =
                    executionContextFactory.create(request);

            String executionId =
                    context.getExecutionId();

            log.info(
                    "[{}] Execution started. mode={}, language={}, environment={}",
                    executionId,
                    request.getMode(),
                    request.getLanguage(),
                    runnerProperties.getEnvironment()
            );

            /*
             * Create temporary workspace.
             */
            workspaceService.createWorkspace(context);

            log.info(
                    "[{}] Workspace created: {}",
                    executionId,
                    context.getWorkspace().getRoot()
            );

            /*
             * Create Docker container when Docker
             * execution environment is enabled.
             */
            if (runnerProperties.getEnvironment()
                    == ExecutionEnvironmentType.DOCKER) {

                createDockerContainer(context);
            }

            /*
             * Persist the execution start.
             *
             * Persistence failures must never break the execution
             * itself, except for duplicate submissions which are
             * rejected intentionally.
             */
            try {

                executionLogService.createExecutionLog(context);

            } catch (DuplicateSubmissionException ex) {

                throw ex;

            } catch (Exception ex) {

                log.warn(
                        "[{}] Unable to persist execution start",
                        context.getExecutionId(),
                        ex
                );
            }

            /*
             * Write source code.
             */
            log.info(
                    "[{}] Writing source code",
                    executionId
            );

            SourceFileWriter writer =
                    sourceFileWriterFactory.getWriter(
                            request.getLanguage()
                    );

            writer.writeSourceFile(context);

            log.info(
                    "[{}] Source code written successfully",
                    executionId
            );

            /*
             * Compile source code.
             */
            log.info(
                    "[{}] Starting compilation",
                    executionId
            );

            LanguageCompiler compiler =
                    compilerFactory.getCompiler(
                            request.getLanguage()
                    );

            CompileResult compileResult =
                    compiler.compile(context);

            log.info(
                    "[{}] Compilation completed. success={}",
                    executionId,
                    compileResult.isSuccess()
            );

            /*
             * Compilation failed.
             */
            if (!compileResult.isSuccess()) {

                log.warn(
                        "[{}] Compilation failed",
                        executionId
                );

                response =
                        responseMapper.fromCompileResult(
                                compileResult
                        );

                return response;
            }

            /*
             * RUN mode.
             *
             * Executes the submitted program against
             * exactly one test case.
             */
            if (request.getMode() == ExecutionMode.RUN) {

                response =
                        executeRunMode(context);

                return response;
            }

            /*
             * JUDGE mode.
             *
             * Executes the program against all
             * configured test cases.
             */
            response =
                    executeJudgeMode(context);

            return response;

        } catch (RuntimeException | Error ex) {

            error = ex;

            throw ex;

        } finally {

            persistExecution(
                    context,
                    response,
                    error
            );

            cleanup(context);

            /*
             * Always release the execution slot after
             * workspace/container cleanup.
             */
            concurrencyLimiter.release();

            if (context != null) {

                log.info(
                        "[{}] Execution slot released",
                        context.getExecutionId()
                );
            } else {

                log.debug(
                        "Execution slot released without execution context"
                );
            }
        }
    }

    /**
     * Persists the execution result in a best-effort fashion.
     *
     * Persistence failures must never interfere with the
     * execution outcome itself.
     */
    private void persistExecution(
            ExecutionContext context,
            ExecuteCodeResponse response,
            Throwable error
    ) {

        if (context == null) {

            return;
        }

        try {

            if (response != null) {

                executionLogService.completeExecution(
                        context,
                        response
                );

            } else if (error != null) {

                executionLogService.completeExecutionWithError(
                        context,
                        error
                );
            }

        } catch (Exception ex) {

            log.warn(
                    "[{}] Unable to persist execution result",
                    context.getExecutionId(),
                    ex
            );
        }
    }

    /**
     * Creates and starts the Docker container for
     * the current execution.
     */
    private void createDockerContainer(
            ExecutionContext context
    ) {

        String executionId =
                context.getExecutionId();

        LanguageMetadata metadata =
                metadataFactory.get(
                        context.getRequest().getLanguage()
                );

        DockerContainer container = null;

        try {

            log.info(
                    "[{}] Creating Docker container. image={}",
                    executionId,
                    metadata.getDockerImage()
            );

            container =
                    dockerService.createContainer(
                            metadata,
                            context.getWorkspace().getRoot()
                    );

            /*
             * Register container immediately so that
             * outer cleanup can always see it.
             */
            context.setDockerContainer(container);

            log.info(
                    "[{}] Docker container created. containerId={}",
                    executionId,
                    container.getId()
            );

            dockerService.startContainer(container);

            log.info(
                    "[{}] Docker container started. containerId={}",
                    executionId,
                    container.getId()
            );

        } catch (Exception ex) {

            log.error(
                    "[{}] Failed to create/start Docker container",
                    executionId,
                    ex
            );

            /*
             * Best-effort cleanup when container creation
             * or startup fails.
             */
            if (container != null) {

                try {

                    dockerService.removeContainer(
                            container
                    );

                    log.info(
                            "[{}] Docker container removed after startup failure. containerId={}",
                            executionId,
                            container.getId()
                    );

                } catch (Exception cleanupEx) {

                    log.warn(
                            "[{}] Unable to remove Docker container after startup failure. containerId={}",
                            executionId,
                            container.getId(),
                            cleanupEx
                    );
                }
            }

            throw ex;
        }
    }

    /**
     * Executes the program in RUN mode.
     */
    private ExecuteCodeResponse executeRunMode(
            ExecutionContext context
    ) {

        String executionId =
                context.getExecutionId();

        log.info(
                "[{}] Starting RUN mode",
                executionId
        );

        context.setCurrentTestCase(
                context.getRequest()
                        .getTestCases()
                        .get(0)
        );

        LanguageRunner runner =
                runnerFactory.getRunner(
                        context.getRequest().getLanguage()
                );

        ExecutionResult executionResult =
                runner.run(context);

        log.info(
                "[{}] RUN mode completed. success={}, timeout={}, memoryLimitExceeded={}, exitCode={}, executionTimeMs={}, memoryUsedMb={}",
                executionId,
                executionResult.isSuccess(),
                executionResult.isTimeout(),
                executionResult.isMemoryLimitExceeded(),
                executionResult.getExitCode(),
                executionResult.getExecutionTimeMillis(),
                executionResult.getMemoryUsedMb()
        );

        return responseMapper.fromExecutionResult(
                executionResult
        );
    }

    /**
     * Executes the program in JUDGE mode.
     */
    private ExecuteCodeResponse executeJudgeMode(
            ExecutionContext context
    ) {

        String executionId =
                context.getExecutionId();

        int totalTestCases =
                context.getRequest()
                        .getTestCases()
                        .size();

        log.info(
                "[{}] Starting JUDGE mode. totalTestCases={}",
                executionId,
                totalTestCases
        );

        JudgeResult judgeResult =
                judgeService.judge(context);

        log.info(
                "[{}] JUDGE mode completed. verdict={}, passed={}/{}, executionTimeMs={}, memoryUsedMb={}",
                executionId,
                judgeResult.getVerdict(),
                judgeResult.getPassed(),
                judgeResult.getTotal(),
                judgeResult.getExecutionTime(),
                judgeResult.getMemoryUsed()
        );

        return responseMapper.fromJudgeResult(
                judgeResult
        );
    }

    /**
     * Performs best-effort cleanup of Docker resources
     * and the temporary workspace.
     *
     * Cleanup failures must never replace the original
     * execution result or exception.
     */
    private void cleanup(
            ExecutionContext context
    ) {

        if (context == null) {

            return;
        }

        String executionId =
                context.getExecutionId();

        log.debug(
                "[{}] Starting execution cleanup",
                executionId
        );

        try {

            DockerContainer container =
                    context.getDockerContainer();

            if (container != null) {

                /*
                 * Stop container.
                 */
                try {

                    log.debug(
                            "[{}] Stopping Docker container. containerId={}",
                            executionId,
                            container.getId()
                    );

                    dockerService.stopContainer(
                            container
                    );

                    log.debug(
                            "[{}] Docker container stopped. containerId={}",
                            executionId,
                            container.getId()
                    );

                } catch (Exception ex) {

                    log.warn(
                            "[{}] Unable to stop Docker container. containerId={}",
                            executionId,
                            container.getId(),
                            ex
                    );
                }

                /*
                 * Remove container.
                 */
                try {

                    log.debug(
                            "[{}] Removing Docker container. containerId={}",
                            executionId,
                            container.getId()
                    );

                    dockerService.removeContainer(
                            container
                    );

                    log.debug(
                            "[{}] Docker container removed. containerId={}",
                            executionId,
                            container.getId()
                    );

                } catch (Exception ex) {

                    log.warn(
                            "[{}] Unable to remove Docker container. containerId={}",
                            executionId,
                            container.getId(),
                            ex
                    );
                }
            }

            /*
             * Cleanup workspace.
             */
            try {

                log.debug(
                        "[{}] Cleaning workspace",
                        executionId
                );

                workspaceService.cleanupWorkspace(
                        context
                );

                log.debug(
                        "[{}] Workspace cleanup completed",
                        executionId
                );

            } catch (Exception ex) {

                log.warn(
                        "[{}] Unable to cleanup workspace",
                        executionId,
                        ex
                );
            }

        } catch (Exception ex) {

            /*
             * Defensive guard.
             *
             * Cleanup must never prevent the concurrency
             * slot from being released.
             */
            log.error(
                    "[{}] Unexpected error during execution cleanup",
                    executionId,
                    ex
            );
        }

        log.debug(
                "[{}] Execution cleanup finished",
                executionId
        );
    }
}