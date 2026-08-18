package com.codexsphere.codearena.execution.docker.stats;

import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.Statistics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DockerMemoryStatsCallbackTest {

    @Test
    void shouldReturnZeroMemoryInitially() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        assertEquals(
                0,
                callback.getPeakMemoryBytes()
        );

        assertEquals(
                0,
                callback.getPeakMemoryMb()
        );
    }

    @Test
    void shouldRecordMemoryUsage() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        Statistics statistics =
                statisticsWithMemory(
                        10L * 1024L * 1024L
                );

        callback.onNext(statistics);

        assertEquals(
                10L * 1024L * 1024L,
                callback.getPeakMemoryBytes()
        );

        assertEquals(
                10,
                callback.getPeakMemoryMb()
        );
    }

    @Test
    void shouldTrackPeakMemory() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        callback.onNext(
                statisticsWithMemory(
                        10L * 1024L * 1024L
                )
        );

        callback.onNext(
                statisticsWithMemory(
                        25L * 1024L * 1024L
                )
        );

        callback.onNext(
                statisticsWithMemory(
                        15L * 1024L * 1024L
                )
        );

        assertEquals(
                25L * 1024L * 1024L,
                callback.getPeakMemoryBytes()
        );

        assertEquals(
                25,
                callback.getPeakMemoryMb()
        );
    }

    @Test
    void shouldIgnoreNullStatistics() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        assertDoesNotThrow(
                () -> callback.onNext(null)
        );

        assertEquals(
                0,
                callback.getPeakMemoryBytes()
        );
    }

    @Test
    void shouldIgnoreStatisticsWithoutMemoryStats() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        Statistics statistics =
                mock(Statistics.class);

        when(
                statistics.getMemoryStats()
        ).thenReturn(null);

        callback.onNext(statistics);

        assertEquals(
                0,
                callback.getPeakMemoryBytes()
        );
    }

    @Test
    void shouldIgnoreMemoryStatsWithoutUsage() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        Statistics statistics =
                mock(
                        Statistics.class,
                        RETURNS_DEEP_STUBS
                );

        when(
                statistics
                        .getMemoryStats()
                        .getUsage()
        ).thenReturn(null);

        callback.onNext(statistics);

        assertEquals(
                0,
                callback.getPeakMemoryBytes()
        );
    }

    @Test
    void shouldRoundMemoryUpToNearestMegabyte() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        callback.onNext(
                statisticsWithMemory(1)
        );

        assertEquals(
                1,
                callback.getPeakMemoryMb()
        );
    }

    @Test
    void shouldConvertExactMegabytesCorrectly() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        callback.onNext(
                statisticsWithMemory(
                        5L * 1024L * 1024L
                )
        );

        assertEquals(
                5,
                callback.getPeakMemoryMb()
        );
    }

    @Test
    void shouldAwaitFirstSampleWithoutBlockingAfterSampleArrives() {

        DockerMemoryStatsCallback callback =
                new DockerMemoryStatsCallback();

        callback.onNext(
                statisticsWithMemory(
                        10L * 1024L * 1024L
                )
        );

        assertDoesNotThrow(
                () ->
                        callback.awaitFirstSample(100)
        );

        assertEquals(
                10,
                callback.getPeakMemoryMb()
        );
    }

    private Statistics statisticsWithMemory(long bytes) {

        Statistics statistics =
                mock(
                        Statistics.class,
                        RETURNS_DEEP_STUBS
                );

        when(
                statistics
                        .getMemoryStats()
                        .getUsage()
        ).thenReturn(bytes);

        return statistics;
    }


}