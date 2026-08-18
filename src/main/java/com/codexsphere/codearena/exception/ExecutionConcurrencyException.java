package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class ExecutionConcurrencyException
        extends BaseException {

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }

    public ExecutionConcurrencyException(
            String message
    ) {
        super(message);
    }

    public ExecutionConcurrencyException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}