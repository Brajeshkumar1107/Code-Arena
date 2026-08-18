package com.codexsphere.codearena.execution.ratelimit;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.ExecutionRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionRateLimiterTest {

    private RunnerProperties runnerProperties;
    private RunnerProperties.RateLimit rateLimit;

    private ExecutionRateLimiter limiter;

    @BeforeEach
    void setUp() {

        runnerProperties =
                mock(RunnerProperties.class);

        rateLimit =
                mock(RunnerProperties.RateLimit.class);

        when(runnerProperties.getRateLimit())
                .thenReturn(rateLimit);

        when(rateLimit.isEnabled())
                .thenReturn(true);

        when(rateLimit.getMaxRequests())
                .thenReturn(3);

        when(rateLimit.getWindowSeconds())
                .thenReturn(60L);

        when(rateLimit.getCleanupIntervalSeconds())
                .thenReturn(300L);

        limiter =
                new ExecutionRateLimiter(
                        runnerProperties
                );
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {

        assertDoesNotThrow(
                () -> limiter.checkRateLimit("client-1")
        );

        assertEquals(
                1,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldAllowRequestsUpToConfiguredLimit() {

        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");

        assertEquals(
                1,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldRejectRequestWhenRateLimitIsExceeded() {

        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");

        ExecutionRateLimitException exception =
                assertThrows(
                        ExecutionRateLimitException.class,
                        () ->
                                limiter.checkRateLimit(
                                        "client-1"
                                )
                );

        assertEquals(
                "Too many execution requests. " +
                        "Please try again later.",
                exception.getMessage()
        );

        assertEquals(
                1,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldMaintainIndependentLimitsForDifferentClients() {

        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");

        limiter.checkRateLimit("client-2");

        assertThrows(
                ExecutionRateLimitException.class,
                () ->
                        limiter.checkRateLimit(
                                "client-1"
                        )
        );

        assertDoesNotThrow(
                () ->
                        limiter.checkRateLimit(
                                "client-2"
                        )
        );

        assertEquals(
                2,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldAllowRequestsWhenRateLimiterIsDisabled() {

        when(rateLimit.isEnabled())
                .thenReturn(false);

        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");

        assertEquals(
                0,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldHandleNullClientId() {

        assertDoesNotThrow(
                () ->
                        limiter.checkRateLimit(null)
        );

        assertEquals(
                1,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldHandleBlankClientId() {

        assertDoesNotThrow(
                () ->
                        limiter.checkRateLimit(" ")
        );

        assertEquals(
                1,
                limiter.getTrackedClientCount()
        );
    }

    @Test
    void shouldRejectInvalidMaxRequests()
            throws Exception {

        when(rateLimit.getMaxRequests())
                .thenReturn(0);

        ExecutionRateLimiter invalidLimiter =
                new ExecutionRateLimiter(
                        runnerProperties
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                invokeInitialize(
                                        invalidLimiter
                                )
                );

        assertEquals(
                "runner.rate-limit.max-requests " +
                        "must be greater than 0",
                exception.getMessage()
        );

        invalidLimiter.shutdown();
    }

    @Test
    void shouldRejectInvalidWindowSeconds()
            throws Exception {

        when(rateLimit.getWindowSeconds())
                .thenReturn(0L);

        ExecutionRateLimiter invalidLimiter =
                new ExecutionRateLimiter(
                        runnerProperties
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                invokeInitialize(
                                        invalidLimiter
                                )
                );

        assertEquals(
                "runner.rate-limit.window-seconds " +
                        "must be greater than 0",
                exception.getMessage()
        );

        invalidLimiter.shutdown();
    }

    @Test
    void shouldRejectInvalidCleanupInterval()
            throws Exception {

        when(rateLimit.getCleanupIntervalSeconds())
                .thenReturn(0L);

        ExecutionRateLimiter invalidLimiter =
                new ExecutionRateLimiter(
                        runnerProperties
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                invokeInitialize(
                                        invalidLimiter
                                )
                );

        assertEquals(
                "runner.rate-limit.cleanup-interval-seconds " +
                        "must be greater than 0",
                exception.getMessage()
        );

        invalidLimiter.shutdown();
    }

    @Test
    void shouldResetWindowAfterWindowExpires()
            throws Exception {

        /*
         * First window.
         */
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-1");

        assertThrows(
                ExecutionRateLimitException.class,
                () ->
                        limiter.checkRateLimit(
                                "client-1"
                        )
        );

        /*
         * Make the configured window very small
         * and initialize a fresh limiter.
         */
        when(rateLimit.getWindowSeconds())
                .thenReturn(1L);

        ExecutionRateLimiter windowLimiter =
                new ExecutionRateLimiter(
                        runnerProperties
                );

        /*
         * We don't use Thread.sleep here because
         * rate-limit tests should ideally be deterministic.
         *
         * The current implementation uses
         * System.currentTimeMillis() internally, so
         * this test verifies the normal window behavior
         * with a short real window.
         */
        windowLimiter.checkRateLimit("client-1");
        windowLimiter.checkRateLimit("client-1");
        windowLimiter.checkRateLimit("client-1");

        Thread.sleep(1100);

        assertDoesNotThrow(
                () ->
                        windowLimiter.checkRateLimit(
                                "client-1"
                        )
        );

        windowLimiter.shutdown();
    }

    @Test
    void shouldRemoveInactiveClients()
            throws Exception {

        /*
         * Use a very small window so the client becomes
         * inactive quickly.
         */
        when(rateLimit.getWindowSeconds())
                .thenReturn(1L);

        ExecutionRateLimiter cleanupLimiter =
                new ExecutionRateLimiter(
                        runnerProperties
                );

        cleanupLimiter.checkRateLimit(
                "inactive-client"
        );

        assertEquals(
                1,
                cleanupLimiter.getTrackedClientCount()
        );

        Thread.sleep(1100);

        invokeCleanup(
                cleanupLimiter
        );

        assertEquals(
                0,
                cleanupLimiter.getTrackedClientCount()
        );

        cleanupLimiter.shutdown();
    }

    @Test
    void shouldTrackMultipleClients() {

        limiter.checkRateLimit("client-1");
        limiter.checkRateLimit("client-2");
        limiter.checkRateLimit("client-3");

        assertEquals(
                3,
                limiter.getTrackedClientCount()
        );
    }

    private void invokeInitialize(
            ExecutionRateLimiter limiter
    ) throws Exception {

        Method method =
                ExecutionRateLimiter.class
                        .getDeclaredMethod(
                                "initialize"
                        );

        method.setAccessible(true);

        try {

            method.invoke(limiter);

        } catch (java.lang.reflect.InvocationTargetException ex) {

            Throwable cause =
                    ex.getCause();

            if (cause instanceof RuntimeException runtimeException) {

                throw runtimeException;
            }

            throw ex;
        }
    }

    private void invokeCleanup(
            ExecutionRateLimiter limiter
    ) throws Exception {

        Method method =
                ExecutionRateLimiter.class
                        .getDeclaredMethod(
                                "cleanupInactiveClients"
                        );

        method.setAccessible(true);

        method.invoke(limiter);
    }
}