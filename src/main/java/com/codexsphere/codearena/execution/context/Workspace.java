package com.codexsphere.codearena.execution.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Builder
public class Workspace {

    private Path root;

    private Path sourceFile;

    private Path executable;

}