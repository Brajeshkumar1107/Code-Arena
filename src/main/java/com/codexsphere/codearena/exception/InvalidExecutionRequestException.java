package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class InvalidExecutionRequestException
        extends BaseException {

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    public InvalidExecutionRequestException(
            String message
    ) {
        super(message);
    }

    public InvalidExecutionRequestException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}