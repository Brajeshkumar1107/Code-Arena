package com.codexsphere.codearena.execution.validation;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.exception.InvalidExecutionRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionRequestValidatorTest {

    private RunnerProperties runnerProperties;
    private RunnerProperties.Limits limits;

    private ExecutionRequestValidator validator;

    @BeforeEach
    void setUp() {

        runnerProperties =
                mock(RunnerProperties.class);

        limits =
                mock(RunnerProperties.Limits.class);

        when(runnerProperties.getLimits())
                .thenReturn(limits);

        when(limits.getMaxSourceCodeBytes())
                .thenReturn(10_000L);

        when(limits.getMaxTestCases())
                .thenReturn(10);

        when(limits.getMaxInputBytes())
                .thenReturn(5_000L);

        when(limits.getMaxExpectedOutputBytes())
                .thenReturn(5_000L);

        validator =
                new ExecutionRequestValidator(
                        runnerProperties
                );
    }

    @Test
    void shouldAcceptValidRunRequest() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("")
                                                .build()
                                )
                        )
                        .build();

        assertDoesNotThrow(
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldAcceptValidJudgeRequest() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.JUDGE)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("5")
                                                .expectedOutput("10")
                                                .build()
                                )
                        )
                        .build();

        assertDoesNotThrow(
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectNullRequest() {

        InvalidExecutionRequestException exception =
                assertThrows(
                        InvalidExecutionRequestException.class,
                        () -> validator.validate(null)
                );

        assertEquals(
                "Execution request cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullMode() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(null)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("")
                                                .build()
                                )
                        )
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectNullLanguage() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(null)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("")
                                                .build()
                                )
                        )
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectBlankSourceCode() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("   ")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("")
                                                .build()
                                )
                        )
                        .build();

        InvalidExecutionRequestException exception =
                assertThrows(
                        InvalidExecutionRequestException.class,
                        () -> validator.validate(request)
                );

        assertEquals(
                "Source code cannot be empty.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectSourceCodeExceedingMaximumSize() {

        when(limits.getMaxSourceCodeBytes())
                .thenReturn(5L);

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(100)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("")
                                                .build()
                                )
                        )
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectNullTestCases() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(null)
                        .build();

        InvalidExecutionRequestException exception =
                assertThrows(
                        InvalidExecutionRequestException.class,
                        () -> validator.validate(request)
                );

        assertEquals(
                "At least one test case is required.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyTestCases() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(List.of())
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectTooManyTestCases() {

        when(limits.getMaxTestCases())
                .thenReturn(2);

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder().build(),
                                        TestCaseRequest.builder().build(),
                                        TestCaseRequest.builder().build()
                                )
                        )
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectNullTestCase() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                Collections.singletonList(null)
                        )
                        .build();

        InvalidExecutionRequestException exception =
                assertThrows(
                        InvalidExecutionRequestException.class,
                        () -> validator.validate(request)
                );

        assertEquals(
                "Test case 1 cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectInputExceedingMaximumSize() {

        when(limits.getMaxInputBytes())
                .thenReturn(5L);

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(input())")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("123456")
                                                .build()
                                )
                        )
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldAcceptNullInput() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input(null)
                                                .build()
                                )
                        )
                        .build();

        assertDoesNotThrow(
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectMissingExpectedOutputInJudgeMode() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.JUDGE)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("5")
                                                .expectedOutput(null)
                                                .build()
                                )
                        )
                        .build();

        InvalidExecutionRequestException exception =
                assertThrows(
                        InvalidExecutionRequestException.class,
                        () -> validator.validate(request)
                );

        assertEquals(
                "Expected output is required for test case 1 in JUDGE mode.",
                exception.getMessage()
        );
    }

    @Test
    void shouldAcceptMissingExpectedOutputInRunMode() {

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.RUN)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("5")
                                                .expectedOutput(null)
                                                .build()
                                )
                        )
                        .build();

        assertDoesNotThrow(
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectExpectedOutputExceedingMaximumSize() {

        when(limits.getMaxExpectedOutputBytes())
                .thenReturn(5L);

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.JUDGE)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("5")
                                                .expectedOutput("123456")
                                                .build()
                                )
                        )
                        .build();

        assertThrows(
                InvalidExecutionRequestException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldAcceptExpectedOutputAtMaximumSize() {

        when(limits.getMaxExpectedOutputBytes())
                .thenReturn(5L);

        ExecuteCodeRequest request =
                ExecuteCodeRequest.builder()
                        .mode(ExecutionMode.JUDGE)
                        .language(Language.PYTHON)
                        .sourceCode("print(10)")
                        .testCases(
                                List.of(
                                        TestCaseRequest.builder()
                                                .input("5")
                                                .expectedOutput("12345")
                                                .build()
                                )
                        )
                        .build();

        assertDoesNotThrow(
                () -> validator.validate(request)
        );
    }
}