package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidTokenException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.INVALID_TOKEN;
    public InvalidTokenException() { super(ErrorCode.INVALID_TOKEN.getMessage()); }
}