package com.codexsphere.codearena.execution.docker.callback;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;

import java.nio.charset.StandardCharsets;

public class DockerResultCallback
        extends ResultCallbackTemplate<DockerResultCallback, Frame> {

    private final StringBuilder stdout =
            new StringBuilder();

    private final StringBuilder stderr =
            new StringBuilder();

    private final long maxStdoutBytes;
    private final long maxStderrBytes;

    private long stdoutBytes;
    private long stderrBytes;

    private volatile boolean outputLimitExceeded;

    public DockerResultCallback(
            long maxStdoutBytes,
            long maxStderrBytes
    ) {

        if (maxStdoutBytes <= 0) {
            throw new IllegalArgumentException(
                    "maxStdoutBytes must be greater than zero"
            );
        }

        if (maxStderrBytes <= 0) {
            throw new IllegalArgumentException(
                    "maxStderrBytes must be greater than zero"
            );
        }

        this.maxStdoutBytes = maxStdoutBytes;
        this.maxStderrBytes = maxStderrBytes;
    }

    @Override
    public void onNext(Frame frame) {

        if (frame == null ||
                frame.getPayload() == null) {

            return;
        }

        byte[] payload =
                frame.getPayload();

        switch (frame.getStreamType()) {

            case STDOUT -> appendStdout(payload);

            case STDERR -> appendStderr(payload);

            default -> {
                // Ignore unsupported Docker stream types.
            }
        }
    }

    private void appendStdout(
            byte[] payload
    ) {

        if (stdoutBytes >= maxStdoutBytes) {

            outputLimitExceeded = true;

            return;
        }

        long remaining =
                maxStdoutBytes - stdoutBytes;

        int bytesToAppend =
                (int) Math.min(
                        remaining,
                        payload.length
                );

        stdout.append(
                new String(
                        payload,
                        0,
                        bytesToAppend,
                        StandardCharsets.UTF_8
                )
        );

        stdoutBytes += bytesToAppend;

        if (bytesToAppend < payload.length) {

            outputLimitExceeded = true;
        }
    }

    private void appendStderr(
            byte[] payload
    ) {

        if (stderrBytes >= maxStderrBytes) {

            outputLimitExceeded = true;

            return;
        }

        long remaining =
                maxStderrBytes - stderrBytes;

        int bytesToAppend =
                (int) Math.min(
                        remaining,
                        payload.length
                );

        stderr.append(
                new String(
                        payload,
                        0,
                        bytesToAppend,
                        StandardCharsets.UTF_8
                )
        );

        stderrBytes += bytesToAppend;

        if (bytesToAppend < payload.length) {

            outputLimitExceeded = true;
        }
    }

    public String getStdout() {
        return stdout.toString();
    }

    public String getStderr() {
        return stderr.toString();
    }

    public boolean isOutputLimitExceeded() {
        return outputLimitExceeded;
    }
}