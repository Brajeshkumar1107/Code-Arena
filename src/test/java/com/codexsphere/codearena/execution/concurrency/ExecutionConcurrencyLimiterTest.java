package com.codexsphere.codearena.execution.concurrency;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.ExecutionConcurrencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionConcurrencyLimiterTest {

    private RunnerProperties runnerProperties;
    private RunnerProperties.Concurrency concurrency;

    private ExecutionConcurrencyLimiter limiter;

    @BeforeEach
    void setUp() {

        runnerProperties =
                mock(RunnerProperties.class);

        concurrency =
                mock(RunnerProperties.Concurrency.class);

        when(runnerProperties.getConcurrency())
                .thenReturn(concurrency);

        when(concurrency.getMaxExecutions())
                .thenReturn(2);

        when(concurrency.getAcquireTimeoutSeconds())
                .thenReturn(1L);

        limiter =
                new ExecutionConcurrencyLimiter(
                        runnerProperties
                );
    }

    @Test
    void shouldAcquireExecutionSlot() {

        limiter.acquire();

        assertEquals(
                1,
                limiter.availableSlots()
        );

        limiter.release();

        assertEquals(
                2,
                limiter.availableSlots()
        );
    }

    @Test
    void shouldReportConfiguredMaximumExecutions() {

        assertEquals(
                2,
                limiter.maxExecutions()
        );
    }

    @Test
    void shouldInitiallyHaveAllExecutionSlotsAvailable() {

        assertEquals(
                2,
                limiter.availableSlots()
        );
    }

    @Test
    void shouldAllowMaximumConfiguredConcurrentExecutions() {

        limiter.acquire();
        limiter.acquire();

        assertEquals(
                0,
                limiter.availableSlots()
        );

        limiter.release();
        limiter.release();

        assertEquals(
                2,
                limiter.availableSlots()
        );
    }

    @Test
    void shouldRejectWhenExecutionSlotIsNotAvailableWithinTimeout()
            throws InterruptedException {

        limiter.acquire();
        limiter.acquire();

        long start =
                System.nanoTime();

        ExecutionConcurrencyException exception =
                assertThrows(
                        ExecutionConcurrencyException.class,
                        () -> limiter.acquire()
                );

        long elapsedMillis =
                TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - start
                );

        assertEquals(
                "Code execution service is busy. Please try again later.",
                exception.getMessage()
        );

        /*
         * The call should have waited approximately
         * the configured timeout instead of failing
         * immediately.
         */
        assertTrue(
                elapsedMillis >= 900,
                "Expected acquire to wait for approximately 1 second"
        );

        limiter.release();
        limiter.release();
    }

    @Test
    void shouldAcquireSlotAfterAnotherExecutionReleasesIt()
            throws InterruptedException {

        limiter.acquire();
        limiter.acquire();

        CountDownLatch started =
                new CountDownLatch(1);

        CountDownLatch acquired =
                new CountDownLatch(1);

        AtomicReference<Throwable> error =
                new AtomicReference<>();

        Thread thread =
                new Thread(() -> {

                    try {

                        started.countDown();

                        limiter.acquire();

                        acquired.countDown();

                    } catch (Throwable ex) {

                        error.set(ex);
                    }
                });

        thread.start();

        assertTrue(
                started.await(
                        1,
                        TimeUnit.SECONDS
                )
        );

        /*
         * Make sure the third execution is actually
         * waiting before releasing a slot.
         */
        Thread.sleep(100);

        assertEquals(
                0,
                limiter.availableSlots()
        );

        /*
         * Release one of the permits held by the
         * main thread.
         */
        limiter.release();

        /*
         * The waiting thread should now acquire
         * the released permit.
         */
        assertTrue(
                acquired.await(
                        1,
                        TimeUnit.SECONDS
                )
        );

        assertNull(
                error.get()
        );

        /*
         * Release the permit held by the waiting thread.
         */
        limiter.release();

        /*
         * Release the remaining permit originally
         * acquired by the main thread.
         */
        limiter.release();

        thread.join(1000);

        assertFalse(
                thread.isAlive()
        );

        assertEquals(
                2,
                limiter.availableSlots()
        );
    }

    @Test
    void shouldHandleInterruptedWaitingThread()
            throws InterruptedException {

        limiter.acquire();
        limiter.acquire();

        CountDownLatch started =
                new CountDownLatch(1);

        AtomicReference<Throwable> error =
                new AtomicReference<>();

        AtomicReference<Boolean> interrupted =
                new AtomicReference<>(
                        false
                );

        Thread thread =
                new Thread(() -> {

                    try {

                        started.countDown();

                        limiter.acquire();

                    } catch (
                            ExecutionConcurrencyException ex
                    ) {

                        error.set(ex);

                        interrupted.set(
                                Thread.currentThread()
                                        .isInterrupted()
                        );
                    }
                });

        thread.start();

        assertTrue(
                started.await(
                        1,
                        TimeUnit.SECONDS
                )
        );

        Thread.sleep(100);

        thread.interrupt();

        thread.join(1000);

        assertFalse(
                thread.isAlive()
        );

        assertNotNull(
                error.get()
        );

        assertInstanceOf(
                ExecutionConcurrencyException.class,
                error.get()
        );

        /*
         * The limiter must restore the interrupted
         * status after catching InterruptedException.
         */
        assertTrue(
                interrupted.get()
        );

        limiter.release();
        limiter.release();

        assertEquals(
                2,
                limiter.availableSlots()
        );
    }

    @Test
    void shouldRejectZeroMaxExecutions() {

        when(concurrency.getMaxExecutions())
                .thenReturn(0);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new ExecutionConcurrencyLimiter(
                                        runnerProperties
                                )
                );

        assertEquals(
                "runner.concurrency.max-executions must be greater than 0",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNegativeMaxExecutions() {

        when(concurrency.getMaxExecutions())
                .thenReturn(-1);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExecutionConcurrencyLimiter(
                                runnerProperties
                        )
        );
    }

    @Test
    void shouldAllowZeroAcquireTimeout() {

        when(concurrency.getAcquireTimeoutSeconds())
                .thenReturn(0L);

        ExecutionConcurrencyLimiter zeroTimeoutLimiter =
                new ExecutionConcurrencyLimiter(
                        runnerProperties
                );

        zeroTimeoutLimiter.acquire();
        zeroTimeoutLimiter.acquire();

        assertThrows(
                ExecutionConcurrencyException.class,
                zeroTimeoutLimiter::acquire
        );

        zeroTimeoutLimiter.release();
        zeroTimeoutLimiter.release();
    }

    @Test
    void shouldRejectNegativeAcquireTimeout() {

        when(concurrency.getAcquireTimeoutSeconds())
                .thenReturn(-1L);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExecutionConcurrencyLimiter(
                                runnerProperties
                        )
        );
    }
}