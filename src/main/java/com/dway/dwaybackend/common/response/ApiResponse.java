package com.dway.dwaybackend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    // One method — pass null for what you don't need
    // @JsonInclude(NON_NULL) removes null fields from JSON automatically
    // data only:    ApiResponse.success(null, taskList)
    // message only: ApiResponse.success("Deleted", null)
    // both:         ApiResponse.success("Created", task)
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Used by GlobalExceptionHandler only
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}