package com.codexsphere.codearena.tatasteel.common.exception;

/**
 * Base runtime exception for the Tata Steel module.
 */
public class TataSteelException extends RuntimeException {

    public TataSteelException(String message) {
        super(message);
    }

    public TataSteelException(String message, Throwable cause) {
        super(message, cause);
    }

}
