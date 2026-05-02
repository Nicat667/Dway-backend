package com.dway.dwaybackend.common.exception.comment;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class CommentNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.COMMENT_NOT_FOUND;
    public CommentNotFoundException() { super(ErrorCode.COMMENT_NOT_FOUND.getMessage()); }
}