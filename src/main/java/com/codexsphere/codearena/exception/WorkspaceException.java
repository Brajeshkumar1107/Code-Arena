package com.codexsphere.codearena.exception;

import org.springframework.http.HttpStatus;

public class WorkspaceException
        extends BaseException {

    public WorkspaceException(
            String message
    ) {
        super(message);
    }

    public WorkspaceException(
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
