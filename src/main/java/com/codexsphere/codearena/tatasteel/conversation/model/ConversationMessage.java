package com.codexsphere.codearena.tatasteel.conversation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Conversation transcript used for auditing and future admin views.
 */
@Entity
@Table(
        name = "ts_conversation_messages",
        indexes = @Index(
                name = "idx_ts_conversation_message_conversation",
                columnList = "conversation_id"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private MessageDirection direction;

    @Column(name = "message_type", nullable = false, length = 30)
    private String messageType;

    @Column(name = "wa_message_id", length = 128)
    private String waMessageId;

    @Column(name = "content", length = 4000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
