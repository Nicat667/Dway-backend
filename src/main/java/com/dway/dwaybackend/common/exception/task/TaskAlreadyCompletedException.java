package com.dway.dwaybackend.common.exception.task;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class TaskAlreadyCompletedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.TASK_ALREADY_COMPLETED;
    public TaskAlreadyCompletedException() { super(ErrorCode.TASK_ALREADY_COMPLETED.getMessage()); }
}