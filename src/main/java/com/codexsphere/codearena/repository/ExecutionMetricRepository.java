package com.codexsphere.codearena.repository;

import com.codexsphere.codearena.entity.ExecutionMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExecutionMetricRepository
        extends JpaRepository<ExecutionMetric, UUID> {
}
