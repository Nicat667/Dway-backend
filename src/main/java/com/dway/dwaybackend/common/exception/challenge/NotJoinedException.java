package com.dway.dwaybackend.common.exception.challenge;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class NotJoinedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.NOT_JOINED;
    public NotJoinedException() { super(ErrorCode.NOT_JOINED.getMessage()); }
}