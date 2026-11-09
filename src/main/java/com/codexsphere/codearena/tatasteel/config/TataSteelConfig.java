package com.codexsphere.codearena.tatasteel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables module scheduling (used by the webhook retry job).
 *
 * <p>Spring Security is intentionally not introduced in this phase because
 * CodeArena currently has no authentication layer.  Adding a global security
 * filter chain now would risk changing the behaviour of the existing runner
 * APIs.  Administrative RBAC will be added in the admin phase together with
 * explicit permit rules for the existing endpoints.</p>
 */
@Configuration
@EnableScheduling
public class TataSteelConfig {
}
