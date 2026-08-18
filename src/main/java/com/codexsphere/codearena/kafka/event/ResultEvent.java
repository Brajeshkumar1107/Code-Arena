package com.codexsphere.codearena.kafka.event;

import com.codexsphere.codearena.enums.Verdict;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultEvent {

    private Long submissionId;

    private Verdict verdict;

    private Long executionTime;

    private Long memoryUsed;

    private Integer passedTestCases;

    private Integer totalTestCases;

}