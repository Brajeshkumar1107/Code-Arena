package com.codexsphere.codearena.entity;

import com.codexsphere.codearena.enums.ExecutionStatus;
import com.codexsphere.codearena.enums.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Optional correlation with the originating submission.
     * Null for ad-hoc executions triggered over HTTP.
     */
    private Long submissionId;

    /**
     * Optional problem the submission belongs to.
     */
    private Long problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExecutionStatus status;

    private String containerId;

    private Integer exitCode;

    private String compilerVersion;

    private String languageRuntime;

    private String dockerImage;

    @Column(length = 3000)
    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

}
