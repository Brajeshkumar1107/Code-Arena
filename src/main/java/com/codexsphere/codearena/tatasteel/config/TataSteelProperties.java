package com.codexsphere.codearena.tatasteel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Module level configuration for Tata Steel.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tatasteel")
public class TataSteelProperties {

    private Webhook webhook = new Webhook();

    @Getter
    @Setter
    public static class Webhook {

        private long retryDelayMs = 300_000L;

        private long retryInitialDelayMs = 60_000L;

    }

}
