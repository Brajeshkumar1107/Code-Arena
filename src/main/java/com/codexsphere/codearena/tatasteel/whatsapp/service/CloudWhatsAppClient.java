package com.codexsphere.codearena.tatasteel.whatsapp.service;

import com.codexsphere.codearena.tatasteel.common.exception.TataSteelException;
import com.codexsphere.codearena.tatasteel.common.exception.WhatsAppApiException;
import com.codexsphere.codearena.tatasteel.common.util.PhoneNumberUtils;
import com.codexsphere.codearena.tatasteel.common.util.TextUtils;
import com.codexsphere.codearena.tatasteel.config.WhatsAppProperties;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.ReplyButton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Official Meta WhatsApp Cloud API client.
 *
 * <p>Only the Graph API is used.  No WhatsApp Web / browser automation is
 * involved.  Access tokens are never logged.</p>
 */
@Component
@Slf4j
public class CloudWhatsAppClient implements WhatsAppClient {

    private static final int MAX_BUTTONS = 3;
    private static final int MAX_BUTTON_TITLE = 20;

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public CloudWhatsAppClient(
            WhatsAppProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.normalizedApiBaseUrl())
                .build();
    }

    @Override
    public void sendText(String to, String text) {
        if (!properties.isEnabled()) {
            log.info("WhatsApp disabled - skipping text message to {}", PhoneNumberUtils.mask(to));
            return;
        }
        requireConfiguration();

        log.debug(
                "Preparing WhatsApp text message to {} ({} characters).",
                PhoneNumberUtils.mask(to),
                text == null ? 0 : text.length()
        );
        Map<String, Object> payload = basePayload(to);
        payload.put("type", "text");
        payload.put("text", Map.of(
                "preview_url", false,
                "body", TextUtils.truncate(text, properties.getConversation().getMaxTextLength())
        ));

        post(payload, to);
    }

    @Override
    public void sendButtons(String to, String body, List<ReplyButton> buttons) {
        if (!properties.isEnabled()) {
            log.info("WhatsApp disabled - skipping interactive message to {}", PhoneNumberUtils.mask(to));
            return;
        }
        requireConfiguration();

        if (buttons == null || buttons.isEmpty()) {
            log.debug("No buttons supplied; falling back to a text message for {}.", PhoneNumberUtils.mask(to));
            sendText(to, body);
            return;
        }
        if (buttons.size() > MAX_BUTTONS) {
            throw new TataSteelException("WhatsApp interactive messages support at most 3 buttons.");
        }

        List<Map<String, Object>> buttonPayloads = new ArrayList<>();
        for (ReplyButton button : buttons) {
            buttonPayloads.add(Map.of(
                    "type", "reply",
                    "reply", Map.of(
                            "id", TextUtils.truncate(button.id(), 256),
                            "title", TextUtils.truncate(button.title(), MAX_BUTTON_TITLE)
                    )
            ));
        }

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "button");
        interactive.put("body", Map.of(
                "text", TextUtils.truncate(body, properties.getConversation().getMaxTextLength())
        ));
        interactive.put("action", Map.of("buttons", buttonPayloads));

        Map<String, Object> payload = basePayload(to);
        payload.put("type", "interactive");
        payload.put("interactive", interactive);

        post(payload, to);
    }

    private Map<String, Object> basePayload(String to) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", PhoneNumberUtils.normalize(to));
        return payload;
    }

    private void post(Map<String, Object> payload, String to) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new WhatsAppApiException("Unable to serialize WhatsApp message payload.", ex);
        }

        String path = "/" + properties.getApiVersion()
                + "/" + properties.getPhoneNumberId()
                + "/messages";

        log.debug(
                "Posting WhatsApp {} message to {} via apiVersion={}.",
                payload.get("type"),
                PhoneNumberUtils.mask(to),
                properties.getApiVersion()
        );

        try {
            restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp message sent to {}", PhoneNumberUtils.mask(to));
        } catch (RestClientResponseException ex) {
            log.error(
                    "WhatsApp Cloud API returned status {} for recipient {}: {}",
                    ex.getStatusCode().value(),
                    PhoneNumberUtils.mask(to),
                    TextUtils.truncate(ex.getResponseBodyAsString(), 300)
            );
            throw new WhatsAppApiException(
                    "WhatsApp Cloud API returned status " + ex.getStatusCode().value(),
                    ex
            );
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message to {}.", PhoneNumberUtils.mask(to), ex);
            throw new WhatsAppApiException("Unable to send WhatsApp message.", ex);
        }
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(properties.getAccessToken())) {
            throw new WhatsAppApiException("WHATSAPP_ACCESS_TOKEN is not configured.");
        }
        if (!StringUtils.hasText(properties.getPhoneNumberId())) {
            throw new WhatsAppApiException("WHATSAPP_PHONE_NUMBER_ID is not configured.");
        }
    }

}
