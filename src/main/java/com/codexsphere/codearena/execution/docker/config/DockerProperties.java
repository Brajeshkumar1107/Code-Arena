package com.codexsphere.codearena.execution.docker.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DockerProperties {

    /**
     * Docker engine host (e.g. {@code tcp://host:2375} or
     * {@code unix:///var/run/docker.sock}).
     *
     * When blank, the docker-java default (honouring the
     * {@code DOCKER_HOST} environment variable) is used.
     */
    private String host = "";

    /**
     * Automatically remove the container after execution.
     */
    private boolean autoRemove = true;

    /**
     * Maximum memory allowed for the container (in MB).
     */
    private long memoryMb = 256;

    /**
     * Maximum CPU cores available to the container.
     */
    private double cpuCount = 1.0;

    /**
     * Maximum number of processes allowed inside the container.
     */
    private long pidsLimit = 64;

    /**
     * Whether networking is enabled inside the container.
     */
    private boolean networkEnabled = false;

    /**
     * Mount the container root filesystem as read-only.
     */
    private boolean readOnlyRootFs = true;

    /**
     * Size of the tmpfs mount for {@code /tmp} inside the container (in MB).
     * Gives programs writable scratch space despite the read-only rootfs.
     */
    private long tmpfsSizeMb = 64;

}