package com.codexsphere.codearena.tatasteel.whatsapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Durable log of every accepted Meta webhook event.
 *
 * <p>The {@code event_key} unique constraint provides idempotency: Meta
 * retries use the same message/status identifier and are therefore ignored.</p>
 */
@Entity
@Table(
        name = "ts_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ts_webhook_event_key",
                columnNames = "event_key"
        ),
        indexes = @Index(
                name = "idx_ts_webhook_status_received",
                columnList = "status, received_at"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "wa_message_id", length = 128)
    private String waMessageId;

    @Column(name = "sender_phone", length = 32)
    private String senderPhone;

    @Column(name = "message_type", length = 30)
    private String messageType;

    @Column(name = "text_body", length = 4000)
    private String textBody;

    @Column(name = "media_id", length = 255)
    private String mediaId;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "filename", length = 255)
    private String filename;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WebhookEventStatus status;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public void markProcessing() {
        this.status = WebhookEventStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markProcessed() {
        this.status = WebhookEventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markSkipped() {
        this.status = WebhookEventStatus.SKIPPED;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String error, int retryCount) {
        this.status = WebhookEventStatus.FAILED;
        this.errorMessage = error == null ? "Unknown error" : error.substring(0, Math.min(error.length(), 1000));
        this.retryCount = retryCount;
    }

    public void markAbandoned() {
        this.status = WebhookEventStatus.ABANDONED;
        this.processedAt = LocalDateTime.now();
    }

}
