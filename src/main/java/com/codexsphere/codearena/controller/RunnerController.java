package com.codexsphere.codearena.controller;

import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.dto.response.ExecuteCodeResponse;
import com.codexsphere.codearena.execution.ExecutionManager;
import com.codexsphere.codearena.execution.ratelimit.ExecutionRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/runner")
@RequiredArgsConstructor
public class RunnerController {

    private final ExecutionManager executionManager;
    private final ExecutionRateLimiter rateLimiter;

    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExecuteCodeResponse execute(
            @Valid @RequestBody ExecuteCodeRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication
    ) {

        /*
         * Authenticated requests from trusted services
         * bypass the public rate limiter.
         */
        if (!isAuthenticated(authentication)) {

            String clientId =
                    resolveClientId(httpRequest);

            rateLimiter.checkRateLimit(clientId);
        }

        return executionManager.execute(request);
    }

    private boolean isAuthenticated(
            Authentication authentication
    ) {

        return authentication != null
                && authentication.isAuthenticated()
                && "runner-service"
                .equals(authentication.getPrincipal());
    }

    private String resolveClientId(
            HttpServletRequest request
    ) {

        return request.getRemoteAddr();
    }
}