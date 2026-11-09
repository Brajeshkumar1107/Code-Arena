package com.codexsphere.codearena.tatasteel.whatsapp.service;

import com.codexsphere.codearena.tatasteel.chatbot.ChatbotService;
import com.codexsphere.codearena.tatasteel.common.util.PhoneNumberUtils;
import com.codexsphere.codearena.tatasteel.config.WhatsAppProperties;
import com.codexsphere.codearena.tatasteel.conversation.model.Conversation;
import com.codexsphere.codearena.tatasteel.conversation.service.ConversationService;
import com.codexsphere.codearena.tatasteel.repository.WebhookEventRepository;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.InboundMessage;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.InboundMessageType;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.OutgoingMessage;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEvent;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Processes a persisted webhook event exactly once.
 *
 * <p>All state changes (conversation, customer, transcript) are committed in
 * one transaction.  Errors are recorded on the event and retried by the
 * scheduled retry job.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppEventProcessor {

    private final WebhookEventRepository webhookEventRepository;
    private final ConversationService conversationService;
    private final ChatbotService chatbotService;
    private final WhatsAppClient whatsAppClient;
    private final WhatsAppProperties whatsAppProperties;

    @Transactional
    public void process(Long eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            log.warn("WhatsApp webhook event {} was not found.", eventId);
            return;
        }

        if (event.getStatus() == WebhookEventStatus.PROCESSED
                || event.getStatus() == WebhookEventStatus.SKIPPED
                || event.getStatus() == WebhookEventStatus.ABANDONED) {
            log.debug("Skipping WhatsApp webhook event {} with terminal status {}.", eventId, event.getStatus());
            return;
        }

        int maxRetries = whatsAppProperties.getWebhook().getMaxRetries();
        if (event.getStatus() == WebhookEventStatus.FAILED && event.getRetryCount() >= maxRetries) {
            event.markAbandoned();
            webhookEventRepository.save(event);
            log.warn(
                    "Abandoning WhatsApp webhook event {} after {} failed attempts.",
                    eventId,
                    event.getRetryCount()
            );
            return;
        }

        if (!"message".equals(event.getEventType())) {
            event.markSkipped();
            webhookEventRepository.save(event);
            log.debug("Skipping non-message WhatsApp webhook event {} of type {}.", eventId, event.getEventType());
            return;
        }

        try {
            log.debug(
                    "Processing WhatsApp event id={} key={} type={} attempt={} sender={}.",
                    eventId,
                    event.getEventKey(),
                    event.getMessageType(),
                    event.getRetryCount() + 1,
                    PhoneNumberUtils.mask(event.getSenderPhone())
            );
            event.markProcessing();
            webhookEventRepository.save(event);

            Conversation conversation = conversationService.getOrCreate(event.getSenderPhone());
            InboundMessage inbound = toInboundMessage(event);

            log.debug(
                    "Conversation id={} for event id={} is in state={} workflow={}.",
                    conversation.getId(),
                    eventId,
                    conversation.getState(),
                    conversation.getWorkflow()
            );

            conversationService.recordInbound(conversation, inbound);

            List<OutgoingMessage> replies = chatbotService.handle(conversation, inbound);
            log.debug(
                    "Chatbot produced {} reply message(s) for conversation id={} event id={}.",
                    replies.size(),
                    conversation.getId(),
                    eventId
            );
            for (OutgoingMessage reply : replies) {
                whatsAppClient.send(event.getSenderPhone(), reply);
                conversationService.recordOutbound(conversation, reply);
            }

            conversationService.touch(conversation);
            event.markProcessed();
            log.info(
                    "Processed WhatsApp event id={} conversationId={} replies={} state={}.",
                    eventId,
                    conversation.getId(),
                    replies.size(),
                    conversation.getState()
            );
        } catch (Exception ex) {
            event.markFailed(safeMessage(ex), event.getRetryCount() + 1);
            log.error(
                    "Failed to process WhatsApp webhook event {} (attempt {}) for sender {}.",
                    eventId,
                    event.getRetryCount(),
                    PhoneNumberUtils.mask(event.getSenderPhone()),
                    ex
            );
        }

        webhookEventRepository.save(event);
    }

    private InboundMessage toInboundMessage(WebhookEvent event) {
        InboundMessageType type;
        try {
            type = InboundMessageType.valueOf(event.getMessageType());
        } catch (Exception ex) {
            type = InboundMessageType.UNKNOWN;
        }

        return new InboundMessage(
                event.getSenderPhone(),
                event.getWaMessageId(),
                type,
                event.getTextBody(),
                event.getMediaId(),
                event.getMimeType(),
                event.getFilename()
        );
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

}
