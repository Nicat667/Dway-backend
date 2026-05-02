package com.dway.dwaybackend.common.exception.achievement;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class AchievementNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.ACHIEVEMENT_NOT_FOUND;
    public AchievementNotFoundException() { super(ErrorCode.ACHIEVEMENT_NOT_FOUND.getMessage()); }
}