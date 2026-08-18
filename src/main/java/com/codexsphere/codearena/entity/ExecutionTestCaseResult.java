package com.codexsphere.codearena.entity;

import com.codexsphere.codearena.enums.Verdict;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "execution_test_case_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTestCaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_log_id", nullable = false)
    private ExecutionLog executionLog;

    @Column(nullable = false)
    private Integer orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Verdict verdict;

    private Long executionTime;

    private Long memoryUsed;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String input;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String expected;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String actual;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stderr;

}
