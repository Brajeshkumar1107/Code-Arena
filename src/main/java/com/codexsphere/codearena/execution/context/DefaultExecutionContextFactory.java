package com.codexsphere.codearena.execution.context;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultExecutionContextFactory
        implements ExecutionContextFactory {

    @Override
    public ExecutionContext create(
            ExecuteCodeRequest request
    ) {

        return ExecutionContext.builder()
                .executionId(
                        UUID.randomUUID().toString()
                )
                .request(request)
                .build();

    }

}