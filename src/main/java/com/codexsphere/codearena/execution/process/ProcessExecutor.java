package com.codexsphere.codearena.execution.process;

import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;

public interface ProcessExecutor {

    ProcessResult execute(ProcessRequest request);

}
