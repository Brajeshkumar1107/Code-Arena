package com.codexsphere.codearena.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseRequest {

    /**
     * Optional.
     * Empty means the program receives no stdin.
     */
    private String input;

    /**
     * Optional in RUN mode.
     * Required in JUDGE mode.
     */
    private String expectedOutput;

    public String getInput() {

        return input == null ? "" : input;

    }

}

