package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class UserBannedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.USER_BANNED;
    public UserBannedException() { super(ErrorCode.USER_BANNED.getMessage()); }
}