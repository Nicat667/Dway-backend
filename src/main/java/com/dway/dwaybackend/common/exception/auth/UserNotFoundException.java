package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
    public UserNotFoundException() { super(ErrorCode.USER_NOT_FOUND.getMessage()); }
}