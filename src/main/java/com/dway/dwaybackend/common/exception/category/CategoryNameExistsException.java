package com.dway.dwaybackend.common.exception.category;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class CategoryNameExistsException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.CATEGORY_NAME_EXISTS;
    public CategoryNameExistsException() { super(ErrorCode.CATEGORY_NAME_EXISTS.getMessage()); }
}