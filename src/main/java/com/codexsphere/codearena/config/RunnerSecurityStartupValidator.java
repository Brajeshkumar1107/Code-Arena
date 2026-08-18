package com.codexsphere.codearena.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Production startup guard.
 *
 * <p>The runner-service token is mandatory in production. If security is
 * enabled and no token is configured, the application must fail fast instead
 * of silently degrading every authenticated request to 401.
 */
@Slf4j
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class RunnerSecurityStartupValidator {

    private final RunnerSecurityProperties securityProperties;

    @PostConstruct
    public void validate() {

        if (securityProperties.isEnabled()
                && (securityProperties.getToken() == null
                || securityProperties.getToken().isBlank())) {

            throw new IllegalStateException(
                    "runner.security.token must be set when "
                            + "runner.security.enabled=true (prod profile). "
                            + "Provide RUNNER_TOKEN."
            );
        }

        log.info(
                "Runner security configuration validated. "
                        + "tokenConfigured={}",
                securityProperties.getToken() != null
                        && !securityProperties.getToken().isBlank()
        );
    }
}
