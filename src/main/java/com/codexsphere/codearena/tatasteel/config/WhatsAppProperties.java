package com.codexsphere.codearena.tatasteel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Tata Steel WhatsApp Cloud API integration.
 *
 * <p>Values are bound from the {@code whatsapp.*} configuration namespace.
 * Secrets are expected to come from environment variables.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private boolean enabled = true;

    private String accessToken;

    private String phoneNumberId;

    private String businessAccountId;

    private String appSecret;

    private String verifyToken;

    private String apiVersion = "v21.0";

    private String apiBaseUrl = "https://graph.facebook.com";

    private Webhook webhook = new Webhook();

    private Conversation conversation = new Conversation();

    public String normalizedApiBaseUrl() {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return "https://graph.facebook.com";
        }
        return apiBaseUrl.replaceAll("/+$", "");
    }

    @Getter
    @Setter
    public static class Webhook {

        private boolean signatureVerificationEnabled = true;

        private int maxRetries = 3;

    }

    @Getter
    @Setter
    public static class Conversation {

        private int sessionTimeoutMinutes = 30;

        private int maxTextLength = 4096;

    }

}
