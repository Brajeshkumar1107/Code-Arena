package com.codexsphere.codearena.execution.process.impl;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult {

    /**
     * Exit code returned by the process.
     */
    private Integer exitCode;

    /**
     * Standard output.
     */
    private String stdout;

    /**
     * Standard error.
     */
    private String stderr;

    /**
     * Whether execution timed out.
     */
    private boolean timeout;

    /**
     * Total execution time in milliseconds.
     */
    private Long executionTimeMillis;

    /**
     * Peak memory used by the process in MB.
     */
    private Long memoryUsedMb;

    /**
     * Whether execution exceeded the configured memory limit.
     */
    private boolean memoryLimitExceeded;

    /**
     * Whether the process exceeded the configured
     * stdout/stderr output limit.
     */
    private boolean outputLimitExceeded;
}