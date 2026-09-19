package com.bankingplatform.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE");
    }
}
