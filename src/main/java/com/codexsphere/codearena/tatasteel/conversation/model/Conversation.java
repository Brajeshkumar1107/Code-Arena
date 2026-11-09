package com.codexsphere.codearena.tatasteel.conversation.model;

import com.codexsphere.codearena.tatasteel.common.model.converter.StringMapConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Durable conversation state for a single WhatsApp user.
 *
 * <p>One row is kept per phone number.  The row is reset (not deleted) when
 * the session expires so that interrupted conversations can be resumed within
 * the configured timeout.</p>
 */
@Entity
@Table(
        name = "ts_conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ts_conversation_phone",
                columnNames = "phone_number"
        ),
        indexes = @Index(
                name = "idx_ts_conversation_last_interaction",
                columnList = "last_interaction_at"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private ConversationState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow", nullable = false, length = 40)
    private ConversationWorkflow workflow;

    @Column(name = "selected_option")
    private Integer selectedOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConversationStatus status;

    @Builder.Default
    @Convert(converter = StringMapConverter.class)
    @Column(name = "attributes_json", length = 4000)
    private Map<String, String> attributes = new HashMap<>();

    @Column(name = "last_interaction_at", nullable = false)
    private LocalDateTime lastInteractionAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        if (lastInteractionAt == null) {
            lastInteractionAt = LocalDateTime.now();
        }
    }

    public Map<String, String> attributesMap() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

}
