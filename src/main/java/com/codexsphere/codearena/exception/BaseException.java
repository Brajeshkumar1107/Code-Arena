package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseException
        extends RuntimeException {
    public abstract HttpStatus getStatus();

    protected BaseException(
            String message
    ) {
        super(message);
    }

    protected BaseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

}
