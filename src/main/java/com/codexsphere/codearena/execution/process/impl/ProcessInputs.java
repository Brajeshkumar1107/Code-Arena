package com.codexsphere.codearena.execution.process.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ProcessInputs {

    public static InputStream fromString(String input) {
        return new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)
        );
    }
}
