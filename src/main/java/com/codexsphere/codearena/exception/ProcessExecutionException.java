package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class ProcessExecutionException extends BaseException {

    public ProcessExecutionException(
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