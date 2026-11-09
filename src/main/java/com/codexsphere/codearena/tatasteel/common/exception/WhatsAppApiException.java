package com.codexsphere.codearena.tatasteel.common.exception;

/**
 * Raised when the WhatsApp Cloud API cannot be called successfully.
 */
public class WhatsAppApiException extends TataSteelException {

    public WhatsAppApiException(String message) {
        super(message);
    }

    public WhatsAppApiException(String message, Throwable cause) {
        super(message, cause);
    }

}
