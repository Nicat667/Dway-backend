package com.dway.dwaybackend.common.exception.challenge;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AlreadyJoinedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.ALREADY_JOINED;
    public AlreadyJoinedException() { super(ErrorCode.ALREADY_JOINED.getMessage()); }
}