package com.dway.dwaybackend.common.exception.task;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class TaskNotCompletedException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.TASK_NOT_COMPLETED;
    public TaskNotCompletedException() { super(ErrorCode.TASK_NOT_COMPLETED.getMessage()); }
}