package com.codexsphere.codearena.execution.compiler.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompileResult {

    private boolean success;

    private String stdout;

    private String stderr;

    private Integer exitCode;

}
