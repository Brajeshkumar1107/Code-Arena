package com.codexsphere.codearena.tatasteel.whatsapp.controller;

import com.codexsphere.codearena.tatasteel.config.WhatsAppProperties;
import com.codexsphere.codearena.tatasteel.whatsapp.dto.WhatsAppWebhookPayload;
import com.codexsphere.codearena.tatasteel.whatsapp.service.WebhookSignatureVerifier;
import com.codexsphere.codearena.tatasteel.whatsapp.service.WhatsAppWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Meta WhatsApp Cloud API webhook.
 *
 * <ul>
 *   <li>{@code GET /webhook} - verification challenge</li>
 *   <li>{@code POST /webhook} - inbound events</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppProperties properties;
    private final WebhookSignatureVerifier signatureVerifier;
    private final WhatsAppWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        log.debug(
                "WhatsApp webhook verification request received (mode={}, tokenConfigured={}).",
                mode,
                StringUtils.hasText(token)
        );
        if (!"subscribe".equals(mode)
                || !StringUtils.hasText(properties.getVerifyToken())
                || !constantTimeEquals(properties.getVerifyToken(), token)) {
            log.warn("WhatsApp webhook verification failed (mode={}).", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("WhatsApp webhook verified successfully.");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(challenge == null ? "" : challenge);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            HttpServletRequest request
    ) {
        byte[] rawBody;
        try {
            rawBody = request.getInputStream().readAllBytes();
        } catch (Exception ex) {
            log.error("Unable to read WhatsApp webhook request body.", ex);
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        log.debug(
                "WhatsApp webhook request received ({} bytes, signaturePresent={}).",
                rawBody.length,
                StringUtils.hasText(signature)
        );

        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Rejected WhatsApp webhook with an invalid signature.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        WhatsAppWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WhatsAppWebhookPayload.class);
        } catch (Exception ex) {
            log.error("Unable to parse WhatsApp webhook payload.", ex);
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        try {
            webhookService.accept(payload, new String(rawBody, StandardCharsets.UTF_8));
            log.debug("WhatsApp webhook accepted for processing.");
        } catch (Exception ex) {
            // The event log is the source of truth.  Return 200 to stop Meta
            // retrying the same payload; the retry job handles failed events.
            log.error("WhatsApp webhook was accepted but processing failed.", ex);
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

}
