package com.codexsphere.codearena.tatasteel.common.util;

/**
 * Small text helpers used by the WhatsApp integration.
 */
public final class TextUtils {

    private TextUtils() {
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

}
