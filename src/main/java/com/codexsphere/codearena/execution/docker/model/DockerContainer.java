package com.codexsphere.codearena.execution.docker.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.time.Instant;

@Builder
@Getter
@Setter
public class DockerContainer {

    /**
     * Docker container ID.
     */
    private String id;

    /**
     * Docker image used to create the container.
     */
    private String image;

    /**
     * Mounted workspace on the host.
     */
    private Path workspace;

    /**
     * Container running status.
     */
    private boolean running;

    /**
     * Container creation time.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Memory limit in MB.
     * (Reserved for future sandboxing)
     */
    private Long memoryLimitMb;

    /**
     * CPU limit.
     * (Reserved for future sandboxing)
     */
    private Double cpuLimit;

}