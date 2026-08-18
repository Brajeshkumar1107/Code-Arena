package com.codexsphere.codearena.execution.judge.model;

import com.codexsphere.codearena.enums.Verdict;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {

    /**
     * Overall verdict.
     */
    private Verdict verdict;

    /**
     * Number of passed test cases.
     */
    private Integer passed;

    /**
     * Total test cases.
     */
    private Integer total;

    /**
     * Total execution time (ms).
     */
    private Long executionTime;

    /**
     * Overall memory usage (MB).
     */
    private Long memoryUsed;

    /**
     * Overall standard output.
     */
    private String stdout;

    /**
     * Overall standard error.
     */
    private String stderr;

    /**
     * Result of every test case.
     */
    private List<TestCaseResult> testCaseResults;
}