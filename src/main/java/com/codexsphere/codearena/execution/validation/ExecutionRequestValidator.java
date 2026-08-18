package com.codexsphere.codearena.execution.validation;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.exception.InvalidExecutionRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutionRequestValidator {

    private final RunnerProperties runnerProperties;

    public void validate(
            ExecuteCodeRequest request
    ) {

        if (request == null) {
            throw new InvalidExecutionRequestException(
                    "Execution request cannot be null."
            );
        }

        validateMode(request);

        validateSourceCode(request);

        validateTestCases(request);
    }

    private void validateMode(
            ExecuteCodeRequest request
    ) {

        if (request.getMode() == null) {
            throw new InvalidExecutionRequestException(
                    "Execution mode is required."
            );
        }

        if (request.getLanguage() == null) {
            throw new InvalidExecutionRequestException(
                    "Programming language is required."
            );
        }
    }

    private void validateSourceCode(
            ExecuteCodeRequest request
    ) {

        String sourceCode =
                request.getSourceCode();

        if (sourceCode == null ||
                sourceCode.isBlank()) {

            throw new InvalidExecutionRequestException(
                    "Source code cannot be empty."
            );
        }

        long size =
                sourceCode.getBytes(
                        StandardCharsets.UTF_8
                ).length;

        long maxSize =
                runnerProperties
                        .getLimits()
                        .getMaxSourceCodeBytes();

        if (size > maxSize) {

            throw new InvalidExecutionRequestException(
                    "Source code exceeds the maximum allowed size of "
                            + maxSize
                            + " bytes."
            );
        }
    }

    private void validateTestCases(
            ExecuteCodeRequest request
    ) {

        List<TestCaseRequest> testCases =
                request.getTestCases();

        if (testCases == null ||
                testCases.isEmpty()) {

            throw new InvalidExecutionRequestException(
                    "At least one test case is required."
            );
        }

        int maxTestCases =
                runnerProperties
                        .getLimits()
                        .getMaxTestCases();

        if (testCases.size() > maxTestCases) {

            throw new InvalidExecutionRequestException(
                    "Maximum allowed test cases is "
                            + maxTestCases
                            + "."
            );
        }

        for (int i = 0; i < testCases.size(); i++) {

            validateTestCase(
                    request,
                    testCases.get(i),
                    i + 1
            );
        }
    }

    private void validateTestCase(
            ExecuteCodeRequest request,
            TestCaseRequest testCase,
            int order
    ) {

        if (testCase == null) {

            throw new InvalidExecutionRequestException(
                    "Test case "
                            + order
                            + " cannot be null."
            );
        }

        validateInput(
                testCase,
                order
        );

        if (request.getMode() == ExecutionMode.JUDGE) {

            validateExpectedOutput(
                    testCase,
                    order
            );
        }
    }

    private void validateInput(
            TestCaseRequest testCase,
            int order
    ) {

        String input =
                testCase.getInput();

        if (input == null) {
            return;
        }

        long size =
                input.getBytes(
                        StandardCharsets.UTF_8
                ).length;

        long maxSize =
                runnerProperties
                        .getLimits()
                        .getMaxInputBytes();

        if (size > maxSize) {

            throw new InvalidExecutionRequestException(
                    "Input for test case "
                            + order
                            + " exceeds the maximum allowed size of "
                            + maxSize
                            + " bytes."
            );
        }
    }

    private void validateExpectedOutput(
            TestCaseRequest testCase,
            int order
    ) {

        String expectedOutput =
                testCase.getExpectedOutput();

        if (expectedOutput == null) {

            throw new InvalidExecutionRequestException(
                    "Expected output is required for test case "
                            + order
                            + " in JUDGE mode."
            );
        }

        long size =
                expectedOutput.getBytes(
                        StandardCharsets.UTF_8
                ).length;

        long maxSize =
                runnerProperties
                        .getLimits()
                        .getMaxExpectedOutputBytes();

        if (size > maxSize) {

            throw new InvalidExecutionRequestException(
                    "Expected output for test case "
                            + order
                            + " exceeds the maximum allowed size of "
                            + maxSize
                            + " bytes."
            );
        }
    }
}