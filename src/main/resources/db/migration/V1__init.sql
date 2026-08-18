CREATE TABLE execution_logs (
    id               BINARY(16) NOT NULL,
    submission_id    BIGINT,
    problem_id       BIGINT,
    language         ENUM('CPP','JAVA','JAVASCRIPT','PYTHON') NOT NULL,
    status           ENUM('COMPLETED','FAILED','QUEUED','RUNNING','SUCCESS') NOT NULL,
    container_id     VARCHAR(255),
    exit_code        INT,
    compiler_version VARCHAR(255),
    language_runtime VARCHAR(255),
    docker_image     VARCHAR(255),
    error_message    VARCHAR(3000),
    started_at       DATETIME(6),
    completed_at     DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_execution_logs_submission_id
    ON execution_logs (submission_id);

CREATE TABLE execution_metrics (
    id                BINARY(16) NOT NULL,
    execution_log_id  BINARY(16) NOT NULL,
    execution_time    BIGINT,
    memory_used       BIGINT,
    cpu_time          BIGINT,
    verdict           ENUM('ACCEPTED','COMPILATION_ERROR','MEMORY_LIMIT_EXCEEDED',
                           'OUTPUT_LIMIT_EXCEEDED','RUNTIME_ERROR',
                           'TIME_LIMIT_EXCEEDED','WRONG_ANSWER'),
    total_test_cases  INT,
    passed_test_cases INT,
    failed_test_case  INT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_execution_metrics_execution_log (execution_log_id),
    CONSTRAINT fk_execution_metrics_execution_log
        FOREIGN KEY (execution_log_id) REFERENCES execution_logs (id)
) ENGINE=InnoDB;

CREATE TABLE test_cases (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    problem_id       BIGINT,
    input            LONGTEXT NOT NULL,
    expected_output  LONGTEXT NOT NULL,
    hidden           BIT(1)   NOT NULL,
    order_no         INT      NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE execution_test_case_results (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    execution_log_id BINARY(16)  NOT NULL,
    order_no         INT         NOT NULL,
    verdict          ENUM('ACCEPTED','COMPILATION_ERROR','MEMORY_LIMIT_EXCEEDED',
                          'OUTPUT_LIMIT_EXCEEDED','RUNTIME_ERROR',
                          'TIME_LIMIT_EXCEEDED','WRONG_ANSWER') NOT NULL,
    execution_time   BIGINT,
    memory_used      BIGINT,
    input            LONGTEXT,
    expected         LONGTEXT,
    actual           LONGTEXT,
    stderr           LONGTEXT,
    PRIMARY KEY (id),
    KEY idx_execution_test_case_results_log (execution_log_id),
    CONSTRAINT fk_execution_test_case_results_log
        FOREIGN KEY (execution_log_id) REFERENCES execution_logs (id)
) ENGINE=InnoDB;
