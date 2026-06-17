package com.marketingagent.exception;

import org.springframework.http.HttpStatus;

public class ExternalProviderException extends ApplicationException {

    public ExternalProviderException(String message) {
        super(ErrorCode.EXTERNAL_PROVIDER_ERROR, HttpStatus.BAD_GATEWAY, message);
    }
}
