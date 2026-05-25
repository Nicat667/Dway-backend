package com.dway.dwaybackend.common.exception.challenge;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ChallengeExpiredException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.CHALLENGE_EXPIRED;
    public ChallengeExpiredException() { super(ErrorCode.CHALLENGE_EXPIRED.getMessage()); }
}