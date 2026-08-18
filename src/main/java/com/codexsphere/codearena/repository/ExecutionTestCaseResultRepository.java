package com.codexsphere.codearena.repository;

import com.codexsphere.codearena.entity.ExecutionTestCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionTestCaseResultRepository
        extends JpaRepository<ExecutionTestCaseResult, Long> {
}
