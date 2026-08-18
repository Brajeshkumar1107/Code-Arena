package com.codexsphere.codearena.execution.process.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class StreamCollector {

    private StreamCollector() {}

    public static String read(InputStream stream)
            throws IOException {

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     stream,
                                     StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {

                builder.append(line)
                        .append(System.lineSeparator());

            }

        }

        return builder.toString();
    }

}