package com.marketingagent.exception;

import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ApplicationException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
