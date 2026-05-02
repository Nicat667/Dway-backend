package com.dway.dwaybackend.common.exception.motivation;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class MotivationNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.MOTIVATION_NOT_FOUND;
    public MotivationNotFoundException() { super(ErrorCode.MOTIVATION_NOT_FOUND.getMessage()); }
}