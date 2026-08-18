package com.codexsphere.codearena.repository;

import com.codexsphere.codearena.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionLogRepository  extends JpaRepository<ExecutionLog, UUID> {

    Optional<ExecutionLog> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionId(Long submissionId);

}
