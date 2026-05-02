package com.dway.dwaybackend.common.exception.ai;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AiServiceException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.AI_SERVICE_ERROR;
    public AiServiceException() { super(ErrorCode.AI_SERVICE_ERROR.getMessage()); }
}