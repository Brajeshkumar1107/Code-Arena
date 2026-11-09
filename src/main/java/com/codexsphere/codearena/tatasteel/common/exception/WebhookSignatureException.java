package com.codexsphere.codearena.tatasteel.common.exception;

/**
 * Raised when a webhook signature cannot be validated.
 */
public class WebhookSignatureException extends TataSteelException {

    public WebhookSignatureException(String message) {
        super(message);
    }

}
