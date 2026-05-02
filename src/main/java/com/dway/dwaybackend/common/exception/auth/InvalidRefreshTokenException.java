package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidRefreshTokenException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.INVALID_REFRESH_TOKEN;
    public InvalidRefreshTokenException() { super(ErrorCode.INVALID_REFRESH_TOKEN.getMessage()); }
}