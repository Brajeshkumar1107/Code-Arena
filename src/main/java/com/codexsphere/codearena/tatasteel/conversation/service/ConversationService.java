package com.codexsphere.codearena.tatasteel.conversation.service;

import com.codexsphere.codearena.tatasteel.common.exception.TataSteelException;
import com.codexsphere.codearena.tatasteel.common.util.PhoneNumberUtils;
import com.codexsphere.codearena.tatasteel.common.util.TextUtils;
import com.codexsphere.codearena.tatasteel.config.WhatsAppProperties;
import com.codexsphere.codearena.tatasteel.conversation.model.*;
import com.codexsphere.codearena.tatasteel.repository.ConversationMessageRepository;
import com.codexsphere.codearena.tatasteel.repository.ConversationRepository;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.InboundMessage;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.OutgoingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private static final int MAX_TRANSCRIPT_LENGTH = 4000;

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final WhatsAppProperties whatsAppProperties;

    /**
     * Returns the durable conversation for a phone number, creating it when
     * necessary.  Expired sessions are reset so the user starts from the main
     * menu, while non-expired sessions are resumed exactly where they stopped.
     */
    @Transactional
    public Conversation getOrCreate(String rawPhoneNumber) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        if (!StringUtils.hasText(phone)) {
            throw new TataSteelException("A valid WhatsApp phone number is required.");
        }

        Conversation existing = conversationRepository.findByPhoneNumber(phone).orElse(null);
        if (existing == null) {
            Conversation created = Conversation.builder()
                    .phoneNumber(phone)
                    .state(ConversationState.NEW)
                    .workflow(ConversationWorkflow.GENERAL)
                    .status(ConversationStatus.ACTIVE)
                    .attributes(new HashMap<>())
                    .lastInteractionAt(LocalDateTime.now())
                    .build();
            Conversation saved = conversationRepository.save(created);
            log.debug(
                    "Created conversation id={} for {} in state={}.",
                    saved.getId(),
                    PhoneNumberUtils.mask(phone),
                    saved.getState()
            );
            return saved;
        }

        if (isExpired(existing)) {
            log.info(
                    "Conversation id={} for {} expired; resetting to a new session.",
                    existing.getId(),
                    PhoneNumberUtils.mask(phone)
            );
            resetState(existing);
        } else {
            log.debug(
                    "Resuming conversation id={} for {} in state={} workflow={}.",
                    existing.getId(),
                    PhoneNumberUtils.mask(phone),
                    existing.getState(),
                    existing.getWorkflow()
            );
        }

        existing.setLastInteractionAt(LocalDateTime.now());
        return conversationRepository.save(existing);
    }

    @Transactional
    public void touch(Conversation conversation) {
        conversation.setLastInteractionAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationMessage recordInbound(Conversation conversation, InboundMessage message) {
        String content = message.text();
        if (!StringUtils.hasText(content) && StringUtils.hasText(message.mediaId())) {
            content = "[" + message.type() + ":" + message.mediaId() + "]";
        }

        ConversationMessage transcript = ConversationMessage.builder()
                .conversationId(conversation.getId())
                .direction(MessageDirection.INBOUND)
                .messageType(message.type() == null ? "UNKNOWN" : message.type().name())
                .waMessageId(message.waMessageId())
                .content(TextUtils.truncate(content, MAX_TRANSCRIPT_LENGTH))
                .build();

        ConversationMessage saved = conversationMessageRepository.save(transcript);
        log.debug(
                "Recorded inbound message id={} for conversation id={} type={}.",
                saved.getId(),
                conversation.getId(),
                saved.getMessageType()
        );
        return saved;
    }

    @Transactional
    public ConversationMessage recordOutbound(Conversation conversation, OutgoingMessage message) {
        ConversationMessage transcript = ConversationMessage.builder()
                .conversationId(conversation.getId())
                .direction(MessageDirection.OUTBOUND)
                .messageType(message.type() == null ? "UNKNOWN" : message.type().name())
                .content(TextUtils.truncate(message.text(), MAX_TRANSCRIPT_LENGTH))
                .build();

        ConversationMessage saved = conversationMessageRepository.save(transcript);
        log.debug(
                "Recorded outbound message id={} for conversation id={} type={}.",
                saved.getId(),
                conversation.getId(),
                saved.getMessageType()
        );
        return saved;
    }

    private boolean isExpired(Conversation conversation) {
        int timeout = whatsAppProperties.getConversation().getSessionTimeoutMinutes();
        if (timeout <= 0 || conversation.getLastInteractionAt() == null) {
            return false;
        }
        return conversation.getLastInteractionAt().isBefore(LocalDateTime.now().minusMinutes(timeout));
    }

    private void resetState(Conversation conversation) {
        conversation.setState(ConversationState.NEW);
        conversation.setWorkflow(ConversationWorkflow.GENERAL);
        conversation.setSelectedOption(null);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.attributesMap().clear();
    }

}
