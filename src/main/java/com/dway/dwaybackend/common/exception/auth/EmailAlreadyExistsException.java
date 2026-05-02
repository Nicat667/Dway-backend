package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EmailAlreadyExistsException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.EMAIL_ALREADY_EXISTS;
    public EmailAlreadyExistsException() { super(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage()); }
}