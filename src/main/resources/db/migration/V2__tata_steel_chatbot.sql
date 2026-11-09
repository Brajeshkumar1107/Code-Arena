CREATE TABLE ts_customers (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    phone_number VARCHAR(32)  NOT NULL,
    name         VARCHAR(120) NOT NULL,
    city         VARCHAR(120),
    state        VARCHAR(120),
    address      VARCHAR(500),
    status       VARCHAR(30)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ts_customer_phone (phone_number)
) ENGINE=InnoDB;

CREATE TABLE ts_conversations (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    phone_number        VARCHAR(32)  NOT NULL,
    customer_id         BIGINT,
    state               VARCHAR(40)  NOT NULL,
    workflow            VARCHAR(40)  NOT NULL,
    selected_option     INT,
    status              VARCHAR(30)  NOT NULL,
    attributes_json     VARCHAR(4000),
    last_interaction_at DATETIME(6)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ts_conversation_phone (phone_number),
    KEY idx_ts_conversation_last_interaction (last_interaction_at)
) ENGINE=InnoDB;

CREATE TABLE ts_conversation_messages (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL,
    direction       VARCHAR(10)  NOT NULL,
    message_type    VARCHAR(30)  NOT NULL,
    wa_message_id   VARCHAR(128),
    content         VARCHAR(4000),
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ts_conversation_message_conversation (conversation_id)
) ENGINE=InnoDB;

CREATE TABLE ts_webhook_events (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    event_key     VARCHAR(255)  NOT NULL,
    event_type    VARCHAR(30)   NOT NULL,
    wa_message_id VARCHAR(128),
    sender_phone  VARCHAR(32),
    message_type  VARCHAR(30),
    text_body     VARCHAR(4000),
    media_id      VARCHAR(255),
    mime_type     VARCHAR(120),
    filename      VARCHAR(255),
    payload_json  LONGTEXT,
    status        VARCHAR(30)   NOT NULL,
    retry_count   INT           NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    received_at   DATETIME(6)   NOT NULL,
    processed_at  DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ts_webhook_event_key (event_key),
    KEY idx_ts_webhook_status_received (status, received_at)
) ENGINE=InnoDB;
