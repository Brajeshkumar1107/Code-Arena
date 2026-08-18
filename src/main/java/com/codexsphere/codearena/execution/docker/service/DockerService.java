package com.codexsphere.codearena.execution.docker.service;

import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.language.LanguageMetadata;
import com.codexsphere.codearena.execution.process.impl.ProcessRequest;
import com.codexsphere.codearena.execution.process.impl.ProcessResult;

import java.nio.file.Path;

public interface DockerService {

    DockerContainer createContainer(LanguageMetadata metadata,
                                    Path workspace);

    void startContainer(DockerContainer container);

    void stopContainer(DockerContainer container);

    void removeContainer(DockerContainer container);

    ProcessResult execute(
            String containerId,
            ProcessRequest request
    );

}