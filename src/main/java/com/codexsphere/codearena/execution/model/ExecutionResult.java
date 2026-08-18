package com.codexsphere.codearena.execution.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /**
     * Indicates whether the execution completed successfully.
     */
    private boolean success;

    /**
     * Program output (stdout).
     */
    private String stdout;

    /**
     * Error output (stderr).
     */
    private String stderr;

    /**
     * Operating system process exit code.
     */
    private Integer exitCode;

    /**
     * True if the process exceeded the configured time limit.
     */
    private boolean timeout;

    /**
     * Total execution time in milliseconds.
     */
    private Long executionTimeMillis;

    /**
     * Peak memory usage in MB.
     * (Will be populated after Docker integration.)
     */
    private Long memoryUsedMb;

    private boolean memoryLimitExceeded;

    private boolean outputLimitExceeded;
}