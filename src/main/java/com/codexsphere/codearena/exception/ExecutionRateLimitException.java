package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class ExecutionRateLimitException
        extends BaseException {

    public ExecutionRateLimitException(
            String message
    ) {
        super(message);
    }

    public ExecutionRateLimitException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}