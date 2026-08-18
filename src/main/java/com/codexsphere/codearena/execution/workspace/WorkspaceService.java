package com.codexsphere.codearena.execution.workspace;

import com.codexsphere.codearena.execution.context.ExecutionContext;

public interface WorkspaceService {
    void createWorkspace(
            ExecutionContext context
    );

    void cleanupWorkspace(
            ExecutionContext context
    );

}