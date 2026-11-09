package com.codexsphere.codearena.tatasteel.whatsapp.service;

import com.codexsphere.codearena.tatasteel.common.util.HashingUtils;
import com.codexsphere.codearena.tatasteel.common.util.PhoneNumberUtils;
import com.codexsphere.codearena.tatasteel.common.util.TextUtils;
import com.codexsphere.codearena.tatasteel.repository.WebhookEventRepository;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.InboundMessage;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.WhatsAppInboundMapper;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.WhatsAppWebhookPayload;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEvent;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Ingests Meta webhook payloads into the durable event log and hands new
 * events to the processor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private static final int MAX_TEXT_LENGTH = 4000;

    private final WhatsAppInboundMapper inboundMapper;
    private final WebhookEventRepository webhookEventRepository;
    private final WhatsAppEventProcessor eventProcessor;

    public void accept(WhatsAppWebhookPayload payload, String rawBody) {
        if (payload == null || payload.entry() == null) {
            log.debug("Ignoring WhatsApp webhook payload without entries.");
            return;
        }

        log.debug("Dispatching WhatsApp webhook payload with {} entries.", payload.entry().size());
        for (WhatsAppWebhookPayload.Entry entry : payload.entry()) {
            if (entry == null || entry.changes() == null) {
                continue;
            }
            for (WhatsAppWebhookPayload.Change change : entry.changes()) {
                if (change == null || change.value() == null) {
                    continue;
                }
                acceptMessages(change.value(), rawBody);
                acceptStatuses(change.value(), rawBody);
            }
        }
    }

    private void acceptMessages(WhatsAppWebhookPayload.Value value, String rawBody) {
        if (value.messages() == null) {
            return;
        }
        for (WhatsAppWebhookPayload.Message message : value.messages()) {
            if (message == null) {
                continue;
            }
            InboundMessage inbound = inboundMapper.from(message);
            String eventKey = eventKey("message", message.id(), rawBody);

            if (webhookEventRepository.existsByEventKey(eventKey)) {
                log.debug("Duplicate WhatsApp message webhook ignored: {}", eventKey);
                continue;
            }

            log.debug(
                    "Persisting WhatsApp message event key={} type={} sender={}.",
                    eventKey,
                    inbound.type(),
                    PhoneNumberUtils.mask(inbound.phoneNumber())
            );

            WebhookEvent event = WebhookEvent.builder()
                    .eventKey(eventKey)
                    .eventType("message")
                    .waMessageId(message.id())
                    .senderPhone(inbound.phoneNumber())
                    .messageType(inbound.type().name())
                    .textBody(TextUtils.truncate(inbound.text(), MAX_TEXT_LENGTH))
                    .mediaId(inbound.mediaId())
                    .mimeType(inbound.mimeType())
                    .filename(inbound.filename())
                    .payloadJson(rawBody)
                    .status(WebhookEventStatus.RECEIVED)
                    .receivedAt(LocalDateTime.now())
                    .build();

            persistAndProcess(event);
        }
    }

    private void acceptStatuses(WhatsAppWebhookPayload.Value value, String rawBody) {
        if (value.statuses() == null) {
            return;
        }
        for (WhatsAppWebhookPayload.Status status : value.statuses()) {
            if (status == null) {
                continue;
            }
            String eventKey = eventKey("status", status.id(), rawBody);
            if (webhookEventRepository.existsByEventKey(eventKey)) {
                log.debug("Duplicate WhatsApp status webhook ignored: {}", eventKey);
                continue;
            }

            WebhookEvent event = WebhookEvent.builder()
                    .eventKey(eventKey)
                    .eventType("status")
                    .waMessageId(status.id())
                    .senderPhone(status.recipientId())
                    .messageType(status.status())
                    .payloadJson(rawBody)
                    .status(WebhookEventStatus.SKIPPED)
                    .receivedAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();

            try {
                webhookEventRepository.save(event);
                log.debug("Recorded WhatsApp status event key={} status={}.", eventKey, status.status());
            } catch (DataIntegrityViolationException ex) {
                log.debug("Concurrent duplicate status webhook ignored: {}", eventKey);
            }
        }
    }

    private void persistAndProcess(WebhookEvent event) {
        WebhookEvent saved;
        try {
            saved = webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent duplicate message webhook ignored: {}", event.getEventKey());
            return;
        }

        log.info(
                "Accepted WhatsApp message event id={} key={} sender={}.",
                saved.getId(),
                saved.getEventKey(),
                PhoneNumberUtils.mask(saved.getSenderPhone())
        );
        eventProcessor.process(saved.getId());
    }

    private String eventKey(String type, String id, String rawBody) {
        if (id != null && !id.isBlank()) {
            return type + ":" + id;
        }
        return type + ":hash:" + HashingUtils.sha256Hex(rawBody == null ? "" : rawBody);
    }

}
