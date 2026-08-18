package com.codexsphere.codearena.execution.docker.security;

import com.github.dockerjava.api.model.HostConfig;

public interface SecurityConfigurer {

    void configure(HostConfig hostConfig);

}
