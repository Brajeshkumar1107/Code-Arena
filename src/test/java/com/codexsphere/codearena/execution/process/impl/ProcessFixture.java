package com.codexsphere.codearena.execution.process.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Cross-platform stand-in for the POSIX shell commands that
 * {@link LocalProcessExecutorTest} needs.
 *
 * <p>The executor itself is OS-agnostic: it launches whatever command it is
 * given and reports stdout, stderr and the exit code. Driving it through
 * {@code sh -c} only worked on Unix, so the suite failed on Windows where no
 * {@code sh} binary exists. Launching this class with the current JVM keeps the
 * assertions byte-exact on every platform, including trailing-newline and
 * UTF-8 behaviour that {@code echo}-style rewrites would disturb.
 */
public final class ProcessFixture {

    private static final int CHUNK = 8192;

    private static final byte[] PATTERN =
            "1234567890"
                    .getBytes(StandardCharsets.UTF_8);

    public static final String UNICODE_SAMPLE =
            "Hello 世界 नमस्ते 🚀";

    private ProcessFixture() {
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {

            writeErr(
                    "missing subcommand"
            );

            System.exit(2);

            return;
        }

        switch (args[0]) {

            case "out" -> writeOut(
                    args[1]
            );

            case "err" -> writeErr(
                    args[1]
            );

            case "lines" -> {
                for (int i = 1; i < args.length; i++) {

                    writeOut(
                            args[i] + "\n"
                    );
                }
            }

            case "both" -> {
                writeOut(
                        args[1]
                );

                writeErr(
                        args[2]
                );
            }

            case "fail" -> {
                writeErr(
                        args[2]
                );

                System.exit(
                        Integer.parseInt(args[1])
                );
            }

            case "cat" -> copyStdinToStdout();

            case "sleep" -> Thread.sleep(
                    Long.parseLong(args[1])
            );

            case "pwd" -> writeOut(
                    System.getProperty("user.dir")
            );

            /*
             * The text is emitted from here rather than passed as an
             * argument: on Windows the console code page mangles non-ASCII
             * argv before the JVM decodes it, which would test the shell
             * instead of the executor.
             */
            case "unicode" -> writeOut(
                    UNICODE_SAMPLE
            );

            case "bigout" -> {
                writePattern(
                        System.out,
                        Integer.parseInt(args[1])
                );

                System.out.flush();
            }

            case "bigerr" -> {
                writePattern(
                        System.err,
                        Integer.parseInt(args[1])
                );

                System.err.flush();
            }

            default -> {
                writeErr(
                        "unknown subcommand: "
                                + args[0]
                );

                System.exit(2);
            }
        }

        System.out.flush();
        System.err.flush();
    }

    private static void writeOut(String value)
            throws IOException {

        System.out.write(
                value.getBytes(StandardCharsets.UTF_8)
        );

        System.out.flush();
    }

    private static void writeErr(String value)
            throws IOException {

        System.err.write(
                value.getBytes(StandardCharsets.UTF_8)
        );

        System.err.flush();
    }

    private static void copyStdinToStdout()
            throws IOException {

        InputStream input =
                System.in;

        byte[] buffer =
                new byte[CHUNK];

        int read;

        while ((read = input.read(buffer)) != -1) {

            System.out.write(
                    buffer,
                    0,
                    read
            );

            System.out.flush();
        }

        System.out.flush();
    }

    /**
     * Writes an exact byte count so the caller can assert on the
     * stream-draining behaviour without depending on shell utilities.
     */
    private static void writePattern(
            OutputStream stream,
            int totalBytes
    ) throws IOException {

        ByteArrayOutputStream generated =
                new ByteArrayOutputStream();

        while (generated.size() < totalBytes) {

            generated.write(
                    PATTERN,
                    0,
                    Math.min(
                            PATTERN.length,
                            totalBytes - generated.size()
                    )
            );
        }

        stream.write(
                generated.toByteArray(),
                0,
                totalBytes
        );
    }
}
