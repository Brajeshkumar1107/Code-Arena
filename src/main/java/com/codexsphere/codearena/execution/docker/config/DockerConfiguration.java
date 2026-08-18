package com.codexsphere.codearena.execution.docker.config;

import com.codexsphere.codearena.config.RunnerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfiguration {

    @Bean
    public DockerClient dockerClient(
            RunnerProperties runnerProperties
    ) {

        DefaultDockerClientConfig.Builder builder =
                DefaultDockerClientConfig
                        .createDefaultConfigBuilder();

        String dockerHost =
                runnerProperties
                        .getDocker()
                        .getHost();

        if (dockerHost != null
                && !dockerHost.isBlank()) {

            builder.withDockerHost(dockerHost);
        }

        DefaultDockerClientConfig config =
                builder.build();

        DockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .build();

        return DockerClientImpl.getInstance(
                config,
                httpClient
        );
    }
}