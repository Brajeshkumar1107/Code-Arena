package com.codexsphere.codearena.dto.response;

import com.codexsphere.codearena.enums.Verdict;
import com.codexsphere.codearena.execution.judge.model.TestCaseResult;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    private boolean success;

    private Verdict verdict;

    @Builder.Default
    private String stdout = "";

    @Builder.Default
    private String stderr = "";

    @Builder.Default
    private Integer passed = 0;

    @Builder.Default
    private Integer total = 0;

    @Builder.Default
    private Long executionTime = 0L;

    @Builder.Default
    private Long memoryUsed = 0L;

    @Builder.Default
    private List<TestCaseResult> testCases =
            Collections.emptyList();
}