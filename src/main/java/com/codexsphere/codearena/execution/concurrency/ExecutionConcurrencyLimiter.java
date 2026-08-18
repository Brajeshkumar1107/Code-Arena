package com.codexsphere.codearena.execution.concurrency;

import com.codexsphere.codearena.config.RunnerProperties;
import com.codexsphere.codearena.exception.ExecutionConcurrencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ExecutionConcurrencyLimiter {

    private final Semaphore semaphore;
    private final RunnerProperties runnerProperties;

    public ExecutionConcurrencyLimiter(
            RunnerProperties runnerProperties
    ) {

        this.runnerProperties = runnerProperties;

        int maxExecutions =
                runnerProperties
                        .getConcurrency()
                        .getMaxExecutions();

        long acquireTimeoutSeconds =
                runnerProperties
                        .getConcurrency()
                        .getAcquireTimeoutSeconds();

        if (maxExecutions <= 0) {

            throw new IllegalArgumentException(
                    "runner.concurrency.max-executions " +
                            "must be greater than 0"
            );
        }

        if (acquireTimeoutSeconds < 0) {

            throw new IllegalArgumentException(
                    "runner.concurrency.acquire-timeout-seconds " +
                            "must not be negative"
            );
        }

        this.semaphore =
                new Semaphore(
                        maxExecutions,
                        true
                );

        log.info(
                "Execution concurrency limiter initialized. " +
                        "maxExecutions={}, acquireTimeoutSeconds={}",
                maxExecutions,
                acquireTimeoutSeconds
        );
    }

    /**
     * Acquires an execution slot.
     *
     * A fair semaphore is used so waiting execution
     * requests are generally served in FIFO order.
     *
     * @throws ExecutionConcurrencyException when no
     * execution slot becomes available within the
     * configured timeout.
     */
    public void acquire() {

        long timeoutSeconds =
                runnerProperties
                        .getConcurrency()
                        .getAcquireTimeoutSeconds();

        log.debug(
                "Waiting for execution slot. " +
                        "availableSlots={}, timeoutSeconds={}",
                semaphore.availablePermits(),
                timeoutSeconds
        );

        try {

            boolean acquired =
                    semaphore.tryAcquire(
                            timeoutSeconds,
                            TimeUnit.SECONDS
                    );

            if (!acquired) {

                log.warn(
                        "Execution rejected because concurrency " +
                                "limit was reached. " +
                                "maxExecutions={}, availableSlots={}",
                        runnerProperties
                                .getConcurrency()
                                .getMaxExecutions(),
                        semaphore.availablePermits()
                );

                throw new ExecutionConcurrencyException(
                        "Code execution service is busy. " +
                                "Please try again later."
                );
            }

            log.debug(
                    "Execution slot acquired. " +
                            "availableSlots={}",
                    semaphore.availablePermits()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            log.warn(
                    "Execution request interrupted while " +
                            "waiting for an execution slot."
            );

            throw new ExecutionConcurrencyException(
                    "Execution request was interrupted " +
                            "while waiting for a slot.",
                    ex
            );
        }
    }

    /**
     * Releases an execution slot.
     *
     * This must be called exactly once after every
     * successful acquire().
     */
    public void release() {

        semaphore.release();

        log.debug(
                "Execution slot released. availableSlots={}",
                semaphore.availablePermits()
        );
    }

    /**
     * Returns the number of currently available
     * execution slots.
     */
    public int availableSlots() {

        return semaphore.availablePermits();
    }

    /**
     * Returns the configured maximum number of
     * concurrent executions.
     */
    public int maxExecutions() {

        return runnerProperties
                .getConcurrency()
                .getMaxExecutions();
    }
}