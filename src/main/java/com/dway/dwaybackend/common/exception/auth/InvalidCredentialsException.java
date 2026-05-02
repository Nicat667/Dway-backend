package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.INVALID_CREDENTIALS;
    public InvalidCredentialsException() { super(ErrorCode.INVALID_CREDENTIALS.getMessage()); }
}