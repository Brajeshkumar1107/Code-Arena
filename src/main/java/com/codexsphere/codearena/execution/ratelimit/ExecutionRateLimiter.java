package com.codexsphere.codearena.execution.ratelimit;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.ExecutionRateLimitException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ExecutionRateLimiter {

    private final RunnerProperties runnerProperties;

    private final Map<String, ClientRateLimit> clients =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "execution-rate-limit-cleanup"
                                );

                        thread.setDaemon(true);

                        return thread;
                    }
            );

    public ExecutionRateLimiter(
            RunnerProperties runnerProperties
    ) {

        this.runnerProperties = runnerProperties;
    }

    @PostConstruct
    public void initialize() {

        RunnerProperties.RateLimit config =
                runnerProperties.getRateLimit();

        validateConfiguration(config);

        if (!config.isEnabled()) {

            log.info(
                    "Execution rate limiter is disabled"
            );

            return;
        }

        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupInactiveClients,
                config.getCleanupIntervalSeconds(),
                config.getCleanupIntervalSeconds(),
                TimeUnit.SECONDS
        );

        log.info(
                "Execution rate limiter initialized. " +
                        "maxRequests={}, windowSeconds={}, " +
                        "cleanupIntervalSeconds={}",
                config.getMaxRequests(),
                config.getWindowSeconds(),
                config.getCleanupIntervalSeconds()
        );
    }

    public void checkRateLimit(
            String clientId
    ) {

        if (!runnerProperties
                .getRateLimit()
                .isEnabled()) {

            return;
        }

        if (clientId == null ||
                clientId.isBlank()) {

            clientId = "unknown";
        }

        RunnerProperties.RateLimit config =
                runnerProperties.getRateLimit();

        long now =
                System.currentTimeMillis();

        ClientRateLimit limit =
                clients.computeIfAbsent(
                        clientId,
                        key -> new ClientRateLimit(now)
                );

        synchronized (limit) {

            /*
             * Start a new rate-limit window if the
             * previous one has expired.
             */
            if (now - limit.getWindowStart()
                    >= config.getWindowSeconds()
                    * 1000L) {

                limit.reset(now);
            }

            int currentRequests =
                    limit.getRequestCount()
                            .incrementAndGet();

            limit.setLastAccessTime(now);

            if (currentRequests >
                    config.getMaxRequests()) {

                limit.getRequestCount()
                        .decrementAndGet();

                log.warn(
                        "Execution rate limit exceeded. " +
                                "clientId={}, requests={}, " +
                                "maxRequests={}, windowSeconds={}",
                        clientId,
                        currentRequests - 1,
                        config.getMaxRequests(),
                        config.getWindowSeconds()
                );

                throw new ExecutionRateLimitException(
                        "Too many execution requests. " +
                                "Please try again later."
                );
            }

            log.debug(
                    "Execution request accepted by rate limiter. " +
                            "clientId={}, requests={}, " +
                            "maxRequests={}",
                    clientId,
                    currentRequests,
                    config.getMaxRequests()
            );
        }
    }

    /**
     * Removes clients that have not made a request
     * for longer than the configured rate-limit window.
     */
    private void cleanupInactiveClients() {

        if (!runnerProperties
                .getRateLimit()
                .isEnabled()) {

            return;
        }

        long now =
                System.currentTimeMillis();

        long expiryMillis =
                runnerProperties
                        .getRateLimit()
                        .getWindowSeconds()
                        * 1000L;

        int removed = 0;

        for (Map.Entry<String, ClientRateLimit> entry
                : clients.entrySet()) {

            ClientRateLimit limit =
                    entry.getValue();

            if (now - limit.getLastAccessTime()
                    >= expiryMillis) {

                if (clients.remove(
                        entry.getKey(),
                        limit
                )) {

                    removed++;
                }
            }
        }

        if (removed > 0) {

            log.debug(
                    "Removed {} inactive rate-limit clients. " +
                            "remainingClients={}",
                    removed,
                    clients.size()
            );
        }
    }

    private void validateConfiguration(
            RunnerProperties.RateLimit config
    ) {

        if (config.getMaxRequests() <= 0) {

            throw new IllegalArgumentException(
                    "runner.rate-limit.max-requests " +
                            "must be greater than 0"
            );
        }

        if (config.getWindowSeconds() <= 0) {

            throw new IllegalArgumentException(
                    "runner.rate-limit.window-seconds " +
                            "must be greater than 0"
            );
        }

        if (config.getCleanupIntervalSeconds() <= 0) {

            throw new IllegalArgumentException(
                    "runner.rate-limit.cleanup-interval-seconds " +
                            "must be greater than 0"
            );
        }
    }

    @PreDestroy
    public void shutdown() {

        log.info(
                "Shutting down execution rate limiter"
        );

        cleanupExecutor.shutdownNow();

        clients.clear();
    }

    public int getTrackedClientCount() {

        return clients.size();
    }

    private static class ClientRateLimit {

        private long windowStart;

        private long lastAccessTime;

        private final AtomicInteger requestCount =
                new AtomicInteger();

        private ClientRateLimit(
                long now
        ) {

            this.windowStart = now;
            this.lastAccessTime = now;
        }

        private long getWindowStart() {
            return windowStart;
        }

        private long getLastAccessTime() {
            return lastAccessTime;
        }

        private void setLastAccessTime(
                long lastAccessTime
        ) {

            this.lastAccessTime =
                    lastAccessTime;
        }

        private AtomicInteger getRequestCount() {
            return requestCount;
        }

        private void reset(
                long newWindowStart
        ) {

            this.windowStart =
                    newWindowStart;

            this.lastAccessTime =
                    newWindowStart;

            requestCount.set(0);
        }
    }
}