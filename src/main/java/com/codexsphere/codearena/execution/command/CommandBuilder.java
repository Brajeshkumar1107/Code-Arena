package com.codexsphere.codearena.execution.command;

import com.codexsphere.codearena.execution.context.ExecutionContext;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;

public interface CommandBuilder {

    ProcessRequest build(
            ExecutionContext context
    );

}