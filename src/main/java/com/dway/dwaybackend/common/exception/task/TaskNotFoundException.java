package com.dway.dwaybackend.common.exception.task;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class TaskNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.TASK_NOT_FOUND;
    public TaskNotFoundException() { super(ErrorCode.TASK_NOT_FOUND.getMessage()); }
}