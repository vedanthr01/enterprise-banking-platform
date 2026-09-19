package com.bankingplatform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all handled business/domain errors. Carries the HTTP status
 * and a stable machine-readable error code so clients can branch on it
 * without parsing the message text.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }

    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
