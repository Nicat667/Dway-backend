package com.dway.dwaybackend.common.exception.post;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class NotLikedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.NOT_LIKED;
    public NotLikedException() { super(ErrorCode.NOT_LIKED.getMessage()); }
}
