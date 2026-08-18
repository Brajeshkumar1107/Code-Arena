package com.codexsphere.codearena.execution.docker.host;

import com.codexsphere.codearena.execution.docker.resource.ResourceConfigurer;
import com.codexsphere.codearena.execution.docker.security.SecurityConfigurer;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultHostConfigBuilder
        implements HostConfigBuilder {

    private static final String WORKSPACE =
            "/workspace";
    private final ResourceConfigurer resourceConfigurer;
    private final SecurityConfigurer securityConfigurer;

    @Override
    public HostConfig build(Path workspace) {

        log.debug(
                "Creating Docker HostConfig for workspace: {}",
                workspace
        );

        HostConfig hostConfig =
                HostConfig.newHostConfig()
                .withBinds(
                        new Bind(
                                workspace
                                        .toAbsolutePath()
                                        .toString(),
                                new Volume(WORKSPACE)
                        )
                );

        resourceConfigurer.configure(
                hostConfig
        );

        securityConfigurer.configure(hostConfig);

        return hostConfig;

    }

}