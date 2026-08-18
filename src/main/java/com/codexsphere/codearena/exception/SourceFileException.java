package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class SourceFileException
        extends BaseException {

    public SourceFileException(
            String message
    ) {
        super(message);
    }

    public SourceFileException(
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