package com.codexsphere.codearena.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long problemId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String input;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private Boolean hidden;

    @Column(nullable = false)
    private Integer orderNo;

}