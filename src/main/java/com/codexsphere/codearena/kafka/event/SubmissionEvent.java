package com.codexsphere.codearena.kafka.event;

import com.codexsphere.codearena.enums.Language;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEvent {

    private Long submissionId;

    private Long problemId;

    private Long contestId;

    private Long userId;

    private Language language;

    private String sourceCode;

    private Integer timeLimit;

    private Integer memoryLimit;

}
