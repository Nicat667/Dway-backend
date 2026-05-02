package com.dway.dwaybackend.common.exception.verification;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidVerificationCodeException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.INVALID_VERIFICATION_CODE;
    public InvalidVerificationCodeException() { super(ErrorCode.INVALID_VERIFICATION_CODE.getMessage()); }
}