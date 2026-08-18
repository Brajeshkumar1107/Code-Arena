package com.codexsphere.codearena.execution.process.impl;

import lombok.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest {

    /**
     * Command to execute.
     */
    private List<String> command;

    /**
     * Host workspace.
     */
    private Path workingDirectory;

    /**
     * Standard input.
     */
    private InputStream input;

    /**
     * Maximum execution time.
     */
    private Duration timeout;

    /**
     * Existing container id.
     * Null means execute locally.
     */
    private String containerId;

    /**
     * Host-side input file mounted inside /workspace.
     */
    private Path inputFile;

}