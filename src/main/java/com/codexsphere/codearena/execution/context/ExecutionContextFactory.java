package com.codexsphere.codearena.execution.context;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;

public interface ExecutionContextFactory {

    ExecutionContext create(ExecuteCodeRequest request);

}