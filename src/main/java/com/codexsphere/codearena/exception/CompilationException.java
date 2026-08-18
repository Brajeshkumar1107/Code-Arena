package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class CompilationException
        extends BaseException {

    public CompilationException(
            String message
    ) {
        super(message);
    }

    public CompilationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

}