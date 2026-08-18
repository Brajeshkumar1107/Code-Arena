package com.codexsphere.codearena.execution.context;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.request.TestCaseRequest;
import com.codexsphere.codearena.entity.ExecutionLog;
import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ExecutionContext {

    private String executionId;

    /**
     * Incoming execution request.
     */
    private ExecuteCodeRequest request;

    /**
     * Temporary workspace.
     */
    private Workspace workspace;

    /**
     * Current test case (used only in Judge mode).
     */
    private TestCaseRequest currentTestCase;

    /**
     * Active Docker container.
     * Null when running locally.
     */
    private DockerContainer dockerContainer;

    /**
     * Persisted execution log entry (null when persistence is disabled
     * or the entry has not been created yet).
     */
    private ExecutionLog executionLog;

}