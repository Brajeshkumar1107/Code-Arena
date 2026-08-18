package com.codexsphere.codearena.entity;


import com.codexsphere.codearena.enums.Verdict;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "execution_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_log_id", nullable = false)
    private ExecutionLog executionLog;

    private Long executionTime;

    private Long memoryUsed;

    private Long cpuTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Verdict verdict;

    private Integer totalTestCases;

    private Integer passedTestCases;

    private Integer failedTestCase;

}