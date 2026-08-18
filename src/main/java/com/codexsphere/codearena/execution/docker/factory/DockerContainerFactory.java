package com.codexsphere.codearena.execution.docker.factory;

import com.codexsphere.codearena.execution.docker.model.DockerContainer;
import com.codexsphere.codearena.execution.language.LanguageMetadata;

import java.nio.file.Path;

public interface DockerContainerFactory {

    DockerContainer create(
            LanguageMetadata metadata,
            Path workspace
    );

}