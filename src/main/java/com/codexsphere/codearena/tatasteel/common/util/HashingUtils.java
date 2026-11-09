package com.codexsphere.codearena.tatasteel.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Hashing helpers used for webhook idempotency keys and signature validation.
 */
public final class HashingUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private HashingUtils() {
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate SHA-256", ex);
        }
    }

    public static String sha256Hex(String data) {
        return sha256Hex(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String hmacSha256Hex(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(data));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate HMAC-SHA256", ex);
        }
    }

}
