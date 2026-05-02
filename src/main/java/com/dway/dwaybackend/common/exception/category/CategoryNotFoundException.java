package com.dway.dwaybackend.common.exception.category;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class CategoryNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.CATEGORY_NOT_FOUND;
    public CategoryNotFoundException() { super(ErrorCode.CATEGORY_NOT_FOUND.getMessage()); }
}