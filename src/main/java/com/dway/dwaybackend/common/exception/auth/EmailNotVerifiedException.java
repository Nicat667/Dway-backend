package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EmailNotVerifiedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.EMAIL_NOT_VERIFIED;
    public EmailNotVerifiedException() { super(ErrorCode.EMAIL_NOT_VERIFIED.getMessage()); }
}