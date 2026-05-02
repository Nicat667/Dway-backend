package com.dway.dwaybackend.common.exception.ai;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AiConfigNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.AI_CONFIG_NOT_FOUND;
    public AiConfigNotFoundException() { super(ErrorCode.AI_CONFIG_NOT_FOUND.getMessage()); }
}