package com.codexsphere.codearena.execution.docker.stats;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Statistics;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DockerMemoryStatsCallback
        extends ResultCallbackTemplate<
        DockerMemoryStatsCallback,
        Statistics> {

    private final AtomicLong peakMemoryBytes =
            new AtomicLong(0);

    private final CountDownLatch firstSample =
            new CountDownLatch(1);

    @Override
    public void onNext(Statistics statistics) {

        if (statistics == null
                || statistics.getMemoryStats() == null) {
            return;
        }

        Long usage =
                statistics.getMemoryStats().getUsage();

        if (usage == null) {
            return;
        }

        peakMemoryBytes.updateAndGet(
                current -> Math.max(current, usage)
        );

        firstSample.countDown();

        log.debug(
                "Docker memory sample: {} MB",
                usage / (1024L * 1024L)
        );
    }

    public void awaitFirstSample(long timeoutMillis) {

        try {

            firstSample.await(
                    timeoutMillis,
                    TimeUnit.MILLISECONDS
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
        }
    }

    public long getPeakMemoryBytes() {
        return peakMemoryBytes.get();
    }

    public long getPeakMemoryMb() {

        long bytes = peakMemoryBytes.get();

        if (bytes <= 0) {
            return 0;
        }

        // Round up so small non-zero usage doesn't become 0 MB.
        return (bytes + (1024L * 1024L) - 1)
                / (1024L * 1024L);
    }
}