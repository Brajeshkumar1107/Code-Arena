package com.codexsphere.codearena.execution;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.enums.ExecutionEnvironmentType;
import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.enums.Language;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionManagerTest {

    private WorkspaceService workspaceService;
    private ExecutionContextFactory executionContextFactory;
    private ExecutionResponseMapper responseMapper;
    private LanguageRunnerFactory runnerFactory;
    private JudgeService judgeService;
    private RunnerProperties runnerProperties;
    private DockerService dockerService;
    private LanguageMetadataFactory metadataFactory;
    private ExecutionConcurrencyLimiter concurrencyLimiter;
    private ExecutionRequestValidator requestValidator;
    private ExecutionLogService executionLogService;
    private ExecutionResult executionResult;
    private ExecuteCodeResponse executeCodeResponse;

    private ExecutionContext context;
    private LanguageCompiler compiler;
    private LanguageRunner runner;
    private SourceFileWriter sourceFileWriter;
    private CompileResult compileResult;

    private ExecuteCodeRequest request;

    private ExecutionManager executionManager;

    @BeforeEach
    void setUp() {

        executionResult =
                mock(ExecutionResult.class);

        executeCodeResponse =
                mock(ExecuteCodeResponse.class);

        workspaceService =
                mock(WorkspaceService.class);

        SourceFileWriterFactory sourceFileWriterFactory = mock(SourceFileWriterFactory.class);

        executionContextFactory =
                mock(ExecutionContextFactory.class);

        responseMapper =
                mock(ExecutionResponseMapper.class);

        LanguageCompilerFactory compilerFactory = mock(LanguageCompilerFactory.class);

        runnerFactory =
                mock(LanguageRunnerFactory.class);

        judgeService =
                mock(JudgeService.class);

        runnerProperties =
                mock(RunnerProperties.class);

        dockerService =
                mock(DockerService.class);

        metadataFactory =
                mock(LanguageMetadataFactory.class);

        concurrencyLimiter =
                mock(ExecutionConcurrencyLimiter.class);

        requestValidator =
                mock(ExecutionRequestValidator.class);

        executionLogService =
                mock(ExecutionLogService.class);

        context =
                mock(
                        ExecutionContext.class,
                        RETURNS_DEEP_STUBS
                );

        compiler =
                mock(LanguageCompiler.class);

        runner =
                mock(LanguageRunner.class);

        sourceFileWriter =
                mock(SourceFileWriter.class);

        compileResult =
                mock(CompileResult.class);

        request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("")
                                                .expectedOutput("")
                                                .build()
                                )
                        )
                        .build();

        /*
         * Default execution environment.
         */
        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.LOCAL
        );

        /*
         * Execution context.
         */
        when(
                executionContextFactory.create(request)
        ).thenReturn(context);

        when(
                context.getExecutionId()
        ).thenReturn("execution-123");

        when(
                context.getRequest()
        ).thenReturn(request);

        when(
                context.getWorkspace()
                        .getRoot()
        ).thenReturn(
                Path.of("/tmp/exec-test")
        );

        /*
         * Source writer.
         */
        when(
                sourceFileWriterFactory.getWriter(
                        Language.PYTHON
                )
        ).thenReturn(sourceFileWriter);

        /*
         * Compiler.
         */
        when(
                compilerFactory.getCompiler(
                        Language.PYTHON
                )
        ).thenReturn(compiler);

        /*
         * IMPORTANT:
         *
         * Default compilation behavior for all tests
         * is successful compilation.
         *
         * Individual tests can override this with:
         *
         * when(compileResult.isSuccess())
         *         .thenReturn(false);
         */
        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        when(
                compileResult.isSuccess()
        ).thenReturn(true);

        when(
                runnerFactory.getRunner(
                        Language.PYTHON
                )
        ).thenReturn(runner);

        /*
         * Compiler.
         */
        when(
                compilerFactory.getCompiler(
                        Language.PYTHON
                )
        ).thenReturn(compiler);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        when(
                compileResult.isSuccess()
        ).thenReturn(true);

        /*
         * Runner.
         */
        when(
                runnerFactory.getRunner(
                        Language.PYTHON
                )
        ).thenReturn(runner);

        when(
                runner.run(context)
        ).thenReturn(executionResult);

        when(
                responseMapper.fromExecutionResult(
                        executionResult
                )
        ).thenReturn(executeCodeResponse);

        when(
                context.getDockerContainer()
        ).thenReturn(null);

        when(
                executionContextFactory.create(request)
        ).thenReturn(context);

        when(
                context.getExecutionId()
        ).thenReturn("execution-123");

        when(
                context.getRequest()
        ).thenReturn(request);

        when(
                context.getWorkspace()
                        .getRoot()
        ).thenReturn(
                Path.of("/tmp/exec-test")
        );

        when(
                context.getDockerContainer()
        ).thenReturn(null);

        /*
         * Create ExecutionManager.
         */
        executionManager =
                new ExecutionManager(
                        workspaceService,
                        sourceFileWriterFactory,
                        executionContextFactory,
                        responseMapper,
                        compilerFactory,
                        runnerFactory,
                        judgeService,
                        runnerProperties,
                        dockerService,
                        metadataFactory,
                        concurrencyLimiter,
                        requestValidator,
                        executionLogService
                );
    }

    @Test
    void shouldValidateRequestBeforeAcquiringExecutionSlot() {

        executionManager.execute(request);

        var inOrder =
                inOrder(
                        requestValidator,
                        concurrencyLimiter
                );

        inOrder.verify(
                requestValidator
        ).validate(request);

        inOrder.verify(
                concurrencyLimiter
        ).acquire();
    }

    @Test
    void shouldCreateExecutionContextAfterAcquiringSlot() {

        executionManager.execute(request);

        var inOrder =
                inOrder(
                        concurrencyLimiter,
                        executionContextFactory
                );

        inOrder.verify(
                concurrencyLimiter
        ).acquire();

        inOrder.verify(
                executionContextFactory
        ).create(request);
    }

    @Test
    void shouldCreateWorkspaceBeforeWritingSourceCode() {

        executionManager.execute(request);

        var inOrder =
                inOrder(
                        workspaceService,
                        sourceFileWriter
                );

        inOrder.verify(
                workspaceService
        ).createWorkspace(context);

        inOrder.verify(
                sourceFileWriter
        ).writeSourceFile(context);
    }

    @Test
    void shouldCompileSourceAfterWritingSourceCode() {

        executionManager.execute(request);

        var inOrder =
                inOrder(
                        sourceFileWriter,
                        compiler
                );

        inOrder.verify(
                sourceFileWriter
        ).writeSourceFile(context);

        inOrder.verify(
                compiler
        ).compile(context);
    }

    @Test
    void shouldExecuteRunModeAfterSuccessfulCompilation() {

        when(
                compileResult.isSuccess()
        ).thenReturn(true);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        ExecutionResult executionResult =
                mock(ExecutionResult.class);

        ExecuteCodeResponse response =
                mock(ExecuteCodeResponse.class);

        when(
                runnerFactory.getRunner(
                        Language.PYTHON
                )
        ).thenReturn(runner);

        when(
                runner.run(context)
        ).thenReturn(executionResult);

        when(
                responseMapper.fromExecutionResult(
                        executionResult
                )
        ).thenReturn(response);

        ExecuteCodeResponse result =
                executionManager.execute(request);

        assertSame(
                response,
                result
        );

        verify(
                runnerFactory
        ).getRunner(Language.PYTHON);

        verify(
                runner
        ).run(context);

        verify(
                responseMapper
        ).fromExecutionResult(
                executionResult
        );
    }

    @Test
    void shouldSetCurrentTestCaseInRunMode() {

        when(
                compileResult.isSuccess()
        ).thenReturn(true);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        when(
                runnerFactory.getRunner(
                        Language.PYTHON
                )
        ).thenReturn(runner);

        when(
                runner.run(context)
        ).thenReturn(
                mock(ExecutionResult.class)
        );

        executionManager.execute(request);

        verify(
                context
        ).setCurrentTestCase(
                request.getTestCases().get(0)
        );
    }

    @Test
    void shouldExecuteJudgeModeAfterSuccessfulCompilation() {

        request.setMode(
                ExecutionMode.JUDGE
        );

        when(
                compileResult.isSuccess()
        ).thenReturn(true);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        JudgeResult judgeResult =
                mock(JudgeResult.class);

        ExecuteCodeResponse response =
                mock(ExecuteCodeResponse.class);

        when(
                judgeService.judge(context)
        ).thenReturn(judgeResult);

        when(
                responseMapper.fromJudgeResult(
                        judgeResult
                )
        ).thenReturn(response);

        ExecuteCodeResponse result =
                executionManager.execute(request);

        assertSame(
                response,
                result
        );

        verify(
                judgeService
        ).judge(context);

        verify(
                responseMapper
        ).fromJudgeResult(
                judgeResult
        );

        verifyNoInteractions(
                runnerFactory
        );

        verifyNoInteractions(
                runner
        );
    }

    @Test
    void shouldReturnCompilationFailureResponse() {

        when(
                compileResult.isSuccess()
        ).thenReturn(false);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        ExecuteCodeResponse response =
                mock(ExecuteCodeResponse.class);

        when(
                responseMapper.fromCompileResult(
                        compileResult
                )
        ).thenReturn(response);

        ExecuteCodeResponse result =
                executionManager.execute(request);

        assertSame(
                response,
                result
        );

        verify(
                responseMapper
        ).fromCompileResult(
                compileResult
        );

        verifyNoInteractions(
                runnerFactory
        );

        verifyNoInteractions(
                runner
        );

        verifyNoInteractions(
                judgeService
        );
    }

    @Test
    void shouldAlwaysReleaseExecutionSlot() {

        when(
                compileResult.isSuccess()
        ).thenReturn(false);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        when(
                responseMapper.fromCompileResult(
                        compileResult
                )
        ).thenReturn(
                mock(ExecuteCodeResponse.class)
        );

        executionManager.execute(request);

        verify(
                concurrencyLimiter
        ).acquire();

        verify(
                concurrencyLimiter
        ).release();
    }

    @Test
    void shouldCleanupWorkspaceAfterExecution() {

        when(
                compileResult.isSuccess()
        ).thenReturn(false);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        when(
                responseMapper.fromCompileResult(
                        compileResult
                )
        ).thenReturn(
                mock(ExecuteCodeResponse.class)
        );

        executionManager.execute(request);

        verify(
                workspaceService
        ).cleanupWorkspace(context);
    }

    @Test
    void shouldReleaseSlotAfterWorkspaceCleanup() {

        when(
                compileResult.isSuccess()
        ).thenReturn(false);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        when(
                responseMapper.fromCompileResult(
                        compileResult
                )
        ).thenReturn(
                mock(ExecuteCodeResponse.class)
        );

        executionManager.execute(request);

        var inOrder =
                inOrder(
                        workspaceService,
                        concurrencyLimiter
                );

        inOrder.verify(
                workspaceService
        ).cleanupWorkspace(context);

        inOrder.verify(
                concurrencyLimiter
        ).release();
    }

    @Test
    void shouldReleaseExecutionSlotWhenCompilationThrowsException() {

        RuntimeException exception =
                new RuntimeException(
                        "Compilation failed unexpectedly"
                );

        when(
                compiler.compile(context)
        ).thenThrow(exception);

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                executionManager.execute(
                                        request
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verify(
                concurrencyLimiter
        ).release();

        verify(
                workspaceService
        ).cleanupWorkspace(context);
    }

    @Test
    void shouldCleanupWorkspaceWhenRunnerThrowsException() {

        when(
                compileResult.isSuccess()
        ).thenReturn(true);

        when(
                compiler.compile(context)
        ).thenReturn(compileResult);

        RuntimeException exception =
                new RuntimeException(
                        "Runner failure"
                );

        when(
                runnerFactory.getRunner(
                        Language.PYTHON
                )
        ).thenReturn(runner);

        when(
                runner.run(context)
        ).thenThrow(exception);

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                executionManager.execute(
                                        request
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verify(
                workspaceService
        ).cleanupWorkspace(context);

        verify(
                concurrencyLimiter
        ).release();
    }

    @Test
    void shouldNotCreateDockerContainerInLocalEnvironment() {

        executionManager.execute(request);

        verifyNoInteractions(
                dockerService
        );

        verifyNoInteractions(
                metadataFactory
        );
    }

    @Test
    void shouldCreateAndStartDockerContainerInDockerEnvironment() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.DOCKER
        );

        LanguageMetadata metadata =
                mock(LanguageMetadata.class);

        DockerContainer container =
                mock(DockerContainer.class);

        when(
                metadataFactory.get(
                        Language.PYTHON
                )
        ).thenReturn(metadata);

        when(
                dockerService.createContainer(
                        eq(metadata),
                        any(Path.class)
                )
        ).thenReturn(container);

        when(
                context.getDockerContainer()
        ).thenReturn(container);

        executionManager.execute(request);

        verify(
                metadataFactory
        ).get(Language.PYTHON);

        verify(
                dockerService
        ).createContainer(
                eq(metadata),
                any(Path.class)
        );

        verify(
                context
        ).setDockerContainer(container);

        verify(
                dockerService
        ).startContainer(container);
    }

    @Test
    void shouldStopAndRemoveDockerContainerDuringCleanup() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.DOCKER
        );

        LanguageMetadata metadata =
                mock(LanguageMetadata.class);

        DockerContainer container =
                mock(DockerContainer.class);

        when(
                metadataFactory.get(
                        Language.PYTHON
                )
        ).thenReturn(metadata);

        when(
                dockerService.createContainer(
                        eq(metadata),
                        any(Path.class)
                )
        ).thenReturn(container);

        when(
                context.getDockerContainer()
        ).thenReturn(container);

        executionManager.execute(request);

        verify(
                dockerService
        ).stopContainer(container);

        verify(
                dockerService
        ).removeContainer(container);
    }

    @Test
    void shouldReleaseSlotWhenDockerStartupFails() {

        when(
                runnerProperties.getEnvironment()
        ).thenReturn(
                ExecutionEnvironmentType.DOCKER
        );

        LanguageMetadata metadata =
                mock(LanguageMetadata.class);

        DockerContainer container =
                mock(DockerContainer.class);

        when(
                metadataFactory.get(
                        Language.PYTHON
                )
        ).thenReturn(metadata);

        when(
                dockerService.createContainer(
                        eq(metadata),
                        any(Path.class)
                )
        ).thenReturn(container);

        RuntimeException exception =
                new RuntimeException(
                        "Docker startup failed"
                );

        doThrow(exception)
                .when(dockerService)
                .startContainer(container);

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                executionManager.execute(
                                        request
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verify(
                concurrencyLimiter
        ).release();

        verify(
                dockerService
        ).removeContainer(container);
    }

    @Test
    void shouldNotCreateExecutionContextWhenValidationFails() {

        RuntimeException exception =
                new RuntimeException(
                        "Invalid execution request"
                );

        doThrow(exception)
                .when(requestValidator)
                .validate(request);

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                executionManager.execute(
                                        request
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verifyNoInteractions(
                concurrencyLimiter
        );

        verifyNoInteractions(
                executionContextFactory
        );

        verifyNoInteractions(
                workspaceService
        );
    }

    @Test
    void shouldNotReleaseSlotWhenAcquireFails() {

        RuntimeException exception =
                new RuntimeException(
                        "Concurrency limit reached"
                );

        doThrow(exception)
                .when(concurrencyLimiter)
                .acquire();

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                executionManager.execute(
                                        request
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verify(
                concurrencyLimiter
        ).acquire();

        verify(
                concurrencyLimiter,
                never()
        ).release();

        verifyNoInteractions(
                executionContextFactory
        );
    }
}