package com.codexsphere.codearena.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "runner.security")
public class RunnerSecurityProperties {

    private boolean enabled = true;

    private String token;
}