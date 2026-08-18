package com.codexsphere.codearena.execution.docker.resource;

import com.github.dockerjava.api.model.HostConfig;

public interface ResourceConfigurer {

    void configure(HostConfig hostConfig);

}
