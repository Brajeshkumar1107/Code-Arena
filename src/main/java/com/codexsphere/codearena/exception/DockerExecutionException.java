package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class DockerExecutionException
        extends BaseException {

    public DockerExecutionException(
            String message
    ) {
        super(message);
    }

    public DockerExecutionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}