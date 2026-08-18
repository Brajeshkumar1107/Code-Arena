package com.codexsphere.codearena.execution.docker.callback;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DockerResultCallbackTest {

    @Test
    void shouldCaptureStdout() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        Frame frame =
                new Frame(
                        StreamType.STDOUT,
                        "Hello World"
                                .getBytes(StandardCharsets.UTF_8)
                );

        callback.onNext(frame);

        assertEquals(
                "Hello World",
                callback.getStdout()
        );

        assertEquals(
                "",
                callback.getStderr()
        );

        assertFalse(
                callback.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldCaptureStderr() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        Frame frame =
                new Frame(
                        StreamType.STDERR,
                        "Error message"
                                .getBytes(StandardCharsets.UTF_8)
                );

        callback.onNext(frame);

        assertEquals(
                "",
                callback.getStdout()
        );

        assertEquals(
                "Error message",
                callback.getStderr()
        );

        assertFalse(
                callback.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldCaptureMultipleStdoutFrames() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        "Hello ".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        "World".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        assertEquals(
                "Hello World",
                callback.getStdout()
        );
    }

    @Test
    void shouldCaptureMultipleStderrFrames() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        callback.onNext(
                new Frame(
                        StreamType.STDERR,
                        "Error ".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        callback.onNext(
                new Frame(
                        StreamType.STDERR,
                        "message".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        assertEquals(
                "Error message",
                callback.getStderr()
        );
    }

    @Test
    void shouldIgnoreNullFrame() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        assertDoesNotThrow(
                () -> callback.onNext(null)
        );

        assertEquals(
                "",
                callback.getStdout()
        );

        assertEquals(
                "",
                callback.getStderr()
        );
    }

    @Test
    void shouldIgnoreFrameWithNullPayload() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        Frame frame =
                new Frame(
                        StreamType.STDOUT,
                        null
                );

        assertDoesNotThrow(
                () -> callback.onNext(frame)
        );

        assertEquals(
                "",
                callback.getStdout()
        );

        assertEquals(
                "",
                callback.getStderr()
        );
    }

    @Test
    void shouldIgnoreUnsupportedStreamType() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        Frame frame =
                new Frame(
                        StreamType.RAW,
                        "ignored".getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        callback.onNext(frame);

        assertEquals(
                "",
                callback.getStdout()
        );

        assertEquals(
                "",
                callback.getStderr()
        );

        assertFalse(
                callback.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldRejectInvalidStdoutLimit() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DockerResultCallback(
                                0,
                                1024
                        )
        );
    }

    @Test
    void shouldRejectInvalidStderrLimit() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DockerResultCallback(
                                1024,
                                0
                        )
        );
    }

    @Test
    void shouldLimitStdout() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        5,
                        1024
                );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        "1234567890"
                                .getBytes(StandardCharsets.UTF_8)
                )
        );

        assertEquals(
                "12345",
                callback.getStdout()
        );

        assertTrue(
                callback.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldLimitStderr() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        5
                );

        callback.onNext(
                new Frame(
                        StreamType.STDERR,
                        "1234567890"
                                .getBytes(StandardCharsets.UTF_8)
                )
        );

        assertEquals(
                "12345",
                callback.getStderr()
        );

        assertTrue(
                callback.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldMarkLimitExceededWhenOutputArrivesAfterLimit() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        5,
                        5
                );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        "12345".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        assertFalse(
                callback.isOutputLimitExceeded()
        );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        "6".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        assertTrue(
                callback.isOutputLimitExceeded()
        );

        assertEquals(
                "12345",
                callback.getStdout()
        );
    }

    @Test
    void shouldAllowOutputExactlyUpToLimit() {

        DockerResultCallback callback =
                new DockerResultCallback(
                        5,
                        5
                );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        "12345".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        assertEquals(
                "12345",
                callback.getStdout()
        );

        assertFalse(
                callback.isOutputLimitExceeded()
        );
    }

    @Test
    void shouldHandleUtf8Output() {

        String output =
                "Hello 世界 नमस्ते 🚀";

        DockerResultCallback callback =
                new DockerResultCallback(
                        1024,
                        1024
                );

        callback.onNext(
                new Frame(
                        StreamType.STDOUT,
                        output.getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        assertEquals(
                output,
                callback.getStdout()
        );
    }
}