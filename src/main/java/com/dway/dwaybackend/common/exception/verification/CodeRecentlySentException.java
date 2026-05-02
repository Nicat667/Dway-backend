package com.dway.dwaybackend.common.exception.verification;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class CodeRecentlySentException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.CODE_RECENTLY_SENT;
    public CodeRecentlySentException() { super(ErrorCode.CODE_RECENTLY_SENT.getMessage()); }
}