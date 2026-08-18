package com.codexsphere.codearena.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RunnerTokenFilter extends OncePerRequestFilter {

    private static final String RUNNER_ENDPOINT =
            "/api/v1/runner/execute";

    private final RunnerSecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!securityProperties.isEnabled()
                || !RUNNER_ENDPOINT.equals(request.getRequestURI())) {

            filterChain.doFilter(request, response);
            return;
        }

        String authorization =
                request.getHeader("Authorization");

        // No token -> anonymous request.
        // Let it continue so the rate limiter can apply.
        if (authorization == null
                || authorization.isBlank()) {

            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Authorization header"
            );
            return;
        }

        String token =
                authorization.substring(7).trim();

        String configuredToken =
                securityProperties.getToken();

        if (configuredToken == null
                || configuredToken.isBlank()
                || token.isBlank()
                || !token.equals(configuredToken)) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid runner token"
            );
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "runner-service",
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}