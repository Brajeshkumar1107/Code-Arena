package com.codexsphere.codearena.execution.process.impl;

import com.codexsphere.codearena.exception.DockerExecutionException;
import com.codexsphere.codearena.exception.ProcessExecutionException;
import com.codexsphere.codearena.execution.docker.service.DockerService;
import com.codexsphere.codearena.execution.process.ProcessExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DockerProcessExecutor
        implements ProcessExecutor {

    private final DockerService dockerService;

    @Override
    public ProcessResult execute(
            ProcessRequest request
    ) {

        try {

            return dockerService.execute(
                    request.getContainerId(),
                    request
            );

        } catch (DockerExecutionException ex) {

            throw ex;

        } catch (ProcessExecutionException ex) {

            throw new DockerExecutionException(
                    "Unable to execute process in Docker.",
                    ex
            );

        } catch (Exception ex) {

            throw new DockerExecutionException(
                    "Unable to execute process in Docker.",
                    ex
            );
        }
    }
}