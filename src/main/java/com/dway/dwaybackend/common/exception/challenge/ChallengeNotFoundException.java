package com.dway.dwaybackend.common.exception.challenge;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ChallengeNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.CHALLENGE_NOT_FOUND;
    public ChallengeNotFoundException() { super(ErrorCode.CHALLENGE_NOT_FOUND.getMessage()); }
}