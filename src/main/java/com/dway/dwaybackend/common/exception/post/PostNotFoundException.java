package com.dway.dwaybackend.common.exception.post;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PostNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.POST_NOT_FOUND;
    public PostNotFoundException() { super(ErrorCode.POST_NOT_FOUND.getMessage()); }
}