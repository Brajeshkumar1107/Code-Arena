package com.codexsphere.codearena.execution.docker.host;

import com.github.dockerjava.api.model.HostConfig;

import java.nio.file.Path;

public interface HostConfigBuilder {

    HostConfig build(Path workspace);

}
