package com.codexsphere.codearena.execution.docker.security;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.execution.docker.config.DockerProperties;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultSecurityConfigurer
        implements SecurityConfigurer {

    private final RunnerProperties runnerProperties;

    @Override
    public void configure(HostConfig hostConfig) {

        DockerProperties docker =
                runnerProperties.getDocker();

        hostConfig
                .withPrivileged(false)

                .withCapDrop(
                        Capability.ALL
                )

                .withSecurityOpts(
                        List.of("no-new-privileges:true")
                )

                .withReadonlyRootfs(
                        docker.isReadOnlyRootFs()
                )

                .withNetworkMode(
                        docker.isNetworkEnabled()
                                ? "bridge"
                                : "none"
                );
    }
}