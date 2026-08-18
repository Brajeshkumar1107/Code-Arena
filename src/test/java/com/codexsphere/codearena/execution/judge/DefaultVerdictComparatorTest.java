package com.codexsphere.codearena.execution.judge;

import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.model.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultVerdictComparatorTest {

    private DefaultVerdictComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new DefaultVerdictComparator();
    }

    @Test
    void shouldReturnAcceptedWhenOutputMatchesExactly() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("20")
                        .build();

        Verdict verdict =
                comparator.compare(
                        testCase,
                        result
                );

        assertEquals(
                Verdict.ACCEPTED,
                verdict
        );
    }

    @Test
    void shouldReturnWrongAnswerWhenOutputDoesNotMatch() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("10")
                        .build();

        Verdict verdict =
                comparator.compare(
                        testCase,
                        result
                );

        assertEquals(
                Verdict.WRONG_ANSWER,
                verdict
        );
    }

    @Test
    void shouldIgnoreLeadingAndTrailingWhitespace() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("   20   ")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldIgnoreTrailingNewline() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("20\n")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldIgnoreLeadingAndTrailingNewlines() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("\n\n20\n\n")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldReturnWrongAnswerWhenValuesDifferOnlyByInternalWhitespace() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20 30")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("20  30")
                        .build();

        assertEquals(
                Verdict.WRONG_ANSWER,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldReturnAcceptedWhenBothOutputsAreEmpty() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldTreatNullExpectedOutputAsEmpty() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput(null)
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldTreatNullActualOutputAsEmpty() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout(null)
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldTreatBothNullOutputsAsEmpty() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput(null)
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout(null)
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldAcceptWhenExpectedOutputIsEmptyNoAssertion() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("20")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldReturnWrongAnswerWhenActualOutputIsEmptyButExpectedIsNot() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("")
                        .build();

        assertEquals(
                Verdict.WRONG_ANSWER,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldBeCaseSensitive() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("Hello")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("hello")
                        .build();

        assertEquals(
                Verdict.WRONG_ANSWER,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldPreserveInternalNewlines() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("10\n20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("10\n20")
                        .build();

        assertEquals(
                Verdict.ACCEPTED,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }

    @Test
    void shouldReturnWrongAnswerWhenInternalNewlinesDiffer() {

        TestCaseRequest testCase =
                TestCaseRequest.builder()
                        .expectedOutput("10\n20")
                        .build();

        ExecutionResult result =
                ExecutionResult.builder()
                        .stdout("10\n30")
                        .build();

        assertEquals(
                Verdict.WRONG_ANSWER,
                comparator.compare(
                        testCase,
                        result
                )
        );
    }
}