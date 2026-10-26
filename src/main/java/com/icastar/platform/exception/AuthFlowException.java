package com.icastar.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthFlowException extends RuntimeException {

    private final HttpStatus status;

    public AuthFlowException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
