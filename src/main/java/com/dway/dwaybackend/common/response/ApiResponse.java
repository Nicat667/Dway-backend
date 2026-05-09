package com.dway.dwaybackend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String path;
    private final LocalDateTime timestamp;
    private final List<FieldError> errors;

    // One method — pass null for what you do not need
    // data only:         ApiResponse.success(null, taskList)
    // message only:      ApiResponse.success("Deleted", null)
    // both:              ApiResponse.success("Created", task)
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> validationError(
            String message, String path, List<FieldError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}