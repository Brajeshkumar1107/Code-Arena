package com.codexsphere.codearena.tatasteel.whatsapp.service;

import com.codexsphere.codearena.tatasteel.config.WhatsAppProperties;
import com.codexsphere.codearena.tatasteel.repository.WebhookEventRepository;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEvent;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retries failed webhook events a bounded number of times.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryJob {

    private final WebhookEventRepository webhookEventRepository;
    private final WhatsAppEventProcessor eventProcessor;
    private final WhatsAppProperties whatsAppProperties;

    @Scheduled(
            fixedDelayString = "${tatasteel.webhook.retry-delay-ms:300000}",
            initialDelayString = "${tatasteel.webhook.retry-initial-delay-ms:60000}"
    )
    public void retryFailedEvents() {
        int maxRetries = whatsAppProperties.getWebhook().getMaxRetries();
        List<WebhookEvent> retryable = webhookEventRepository
                .findTop50ByStatusAndRetryCountLessThanOrderByReceivedAtAsc(
                        WebhookEventStatus.FAILED,
                        maxRetries
                );

        if (retryable.isEmpty()) {
            log.debug("No failed WhatsApp webhook events to retry.");
            return;
        }

        log.info("Retrying {} failed WhatsApp webhook event(s).", retryable.size());
        for (WebhookEvent event : retryable) {
            log.debug(
                    "Retrying WhatsApp webhook event id={} attempt={} maxRetries={}.",
                    event.getId(),
                    event.getRetryCount() + 1,
                    maxRetries
            );
            try {
                eventProcessor.process(event.getId());
            } catch (Exception ex) {
                log.error("Webhook retry failed for event {}.", event.getId(), ex);
            }
        }
    }

}
