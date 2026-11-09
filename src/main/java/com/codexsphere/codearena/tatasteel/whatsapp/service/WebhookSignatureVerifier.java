package com.codexsphere.codearena.tatasteel.whatsapp.service;

import com.codexsphere.codearena.tatasteel.common.util.HashingUtils;
import com.codexsphere.codearena.tatasteel.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Validates the {@code X-Hub-Signature-256} header sent by Meta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureVerifier {

    private static final String PREFIX = "sha256=";

    private final WhatsAppProperties properties;

    public boolean isValid(byte[] payload, String signatureHeader) {
        if (!properties.getWebhook().isSignatureVerificationEnabled()) {
            log.warn("WhatsApp webhook signature verification is disabled. Do not use this in production.");
            return true;
        }

        String secret = properties.getAppSecret();
        if (!StringUtils.hasText(secret)) {
            log.error("WHATSAPP_APP_SECRET is not configured; rejecting webhook.");
            return false;
        }
        if (!StringUtils.hasText(signatureHeader)) {
            log.debug("Missing X-Hub-Signature-256 header; rejecting webhook.");
            return false;
        }

        String provided = signatureHeader.trim();
        if (provided.startsWith(PREFIX)) {
            provided = provided.substring(PREFIX.length());
        }
        if (!StringUtils.hasText(provided)) {
            log.debug("Empty X-Hub-Signature-256 value; rejecting webhook.");
            return false;
        }

        String expected = HashingUtils.hmacSha256Hex(secret, payload);
        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            log.debug("WhatsApp webhook signature mismatch (payloadBytes={}).", payload == null ? 0 : payload.length);
        }
        return valid;
    }

}
