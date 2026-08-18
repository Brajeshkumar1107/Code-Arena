package com.codexsphere.codearena.execution.process.impl;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class LimitedStreamReader {

    private final InputStream inputStream;
    private final long maxBytes;

    public Result read() throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];

        long totalBytes = 0;

        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {

            long remaining =
                    maxBytes - totalBytes;

            if (remaining <= 0) {

                return new Result(
                        output.toString(
                                StandardCharsets.UTF_8
                        ),
                        true
                );
            }

            int bytesToWrite =
                    (int) Math.min(
                            bytesRead,
                            remaining
                    );

            output.write(
                    buffer,
                    0,
                    bytesToWrite
            );

            totalBytes += bytesToWrite;

            if (bytesToWrite < bytesRead) {

                return new Result(
                        output.toString(
                                StandardCharsets.UTF_8
                        ),
                        true
                );
            }
        }

        return new Result(
                output.toString(
                        StandardCharsets.UTF_8
                ),
                false
        );
    }

    public record Result(
            String output,
            boolean limitExceeded
    ) {
    }
}