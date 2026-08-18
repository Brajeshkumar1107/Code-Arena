package com.codexsphere.codearena.dto.request;

import com.codexsphere.codearena.enums.ExecutionMode;
import com.codexsphere.codearena.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    @NotNull
    private ExecutionMode mode;

    @NotNull
    private Language language;

    @NotBlank
    private String sourceCode;

    @NotEmpty
    private List<TestCaseRequest> testCases;

    /**
     * Optional submission identifier used to correlate an execution with
     * its originating submission and to guard against duplicate replays.
     */
    private Long submissionId;

    /**
     * Optional problem identifier the submission belongs to.
     */
    private Long problemId;
}