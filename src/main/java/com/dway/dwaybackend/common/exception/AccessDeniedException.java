package com.dway.dwaybackend.common.exception;

import lombok.Getter;

@Getter
public class AccessDeniedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

    public AccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED.getMessage());
    }
}