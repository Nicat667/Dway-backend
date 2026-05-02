package com.dway.dwaybackend.common.exception.motivation;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class MotivationAlreadyExistsException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.MOTIVATION_ALREADY_EXISTS;
    public MotivationAlreadyExistsException() { super(ErrorCode.MOTIVATION_ALREADY_EXISTS.getMessage()); }
}