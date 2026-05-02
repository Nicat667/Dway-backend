package com.dway.dwaybackend.common.exception.ai;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AiSessionNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.AI_SESSION_NOT_FOUND;
    public AiSessionNotFoundException() { super(ErrorCode.AI_SESSION_NOT_FOUND.getMessage()); }
}