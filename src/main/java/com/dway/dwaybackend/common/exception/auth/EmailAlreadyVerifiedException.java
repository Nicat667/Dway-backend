package com.dway.dwaybackend.common.exception.auth;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EmailAlreadyVerifiedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.EMAIL_ALREADY_VERIFIED;
    public EmailAlreadyVerifiedException() {
        super(ErrorCode.EMAIL_ALREADY_VERIFIED.getMessage());
    }
}