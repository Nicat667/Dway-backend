package com.dway.dwaybackend.common.exception.post;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AlreadyLikedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.ALREADY_LIKED;
    public AlreadyLikedException() { super(ErrorCode.ALREADY_LIKED.getMessage()); }
}