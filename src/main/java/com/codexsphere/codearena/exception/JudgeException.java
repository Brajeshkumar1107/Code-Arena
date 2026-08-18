package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class JudgeException
        extends BaseException {

    public JudgeException(
            String message
    ) {
        super(message);
    }

    public JudgeException(
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