package com.codexsphere.codearena.dto.response;

import com.codexsphere.codearena.enums.Verdict;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResponse {

    private Integer order;

    private Verdict verdict;

    private Long executionTime;

    private String stdout;

}