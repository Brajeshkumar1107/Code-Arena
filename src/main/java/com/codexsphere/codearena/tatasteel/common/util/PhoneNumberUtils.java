package com.codexsphere.codearena.tatasteel.common.util;

/**
 * Utility methods for WhatsApp / E.164 style phone numbers.
 */
public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    /**
     * Normalizes a phone number to its digits-only WhatsApp form.
     * WhatsApp webhook payloads already use digits without a leading '+'.
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    public static String mask(String value) {
        String normalized = normalize(value);
        if (normalized.length() <= 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

}
