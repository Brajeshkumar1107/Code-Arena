package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class ExecutionException
        extends BaseException {

    public ExecutionException(
            String message
    ) {
        super(message);
    }

    public ExecutionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}