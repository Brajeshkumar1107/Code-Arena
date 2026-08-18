package com.codexsphere.codearena.execution.judge.model;

import com.codexsphere.codearena.enums.Verdict;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {

    /**
     * Order of execution.
     */
    private Integer order;

    /**
     * Input provided.
     */
    private String input;

    /**
     * Expected output.
     */
    private String expected;

    /**
     * Actual output.
     */
    private String actual;

    /**
     * Standard error produced by the program.
     */
    private String stderr;

    /**
     * Verdict.
     */
    private Verdict verdict;

    /**
     * Execution time (ms).
     */
    private Long executionTime;

    /**
     * Memory used (MB).
     */
    private Long memoryUsed;
}