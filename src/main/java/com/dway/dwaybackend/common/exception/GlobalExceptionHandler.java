package com.dway.dwaybackend.common.exception;

import com.dway.dwaybackend.common.exception.achievement.AchievementNotFoundException;
import com.dway.dwaybackend.common.exception.ai.AiConfigNotFoundException;
import com.dway.dwaybackend.common.exception.ai.AiServiceException;
import com.dway.dwaybackend.common.exception.ai.AiSessionNotFoundException;
import com.dway.dwaybackend.common.exception.auth.*;
import com.dway.dwaybackend.common.exception.category.*;
import com.dway.dwaybackend.common.exception.challenge.*;
import com.dway.dwaybackend.common.exception.comment.*;
import com.dway.dwaybackend.common.exception.motivation.*;
import com.dway.dwaybackend.common.exception.partner.*;
import com.dway.dwaybackend.common.exception.post.*;
import com.dway.dwaybackend.common.exception.task.*;
import com.dway.dwaybackend.common.exception.verification.*;
import com.dway.dwaybackend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Auth ─────────────────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            UserNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            EmailAlreadyExistsException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            EmailAlreadyVerifiedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            EmailNotVerifiedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            UserBannedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            InvalidTokenException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Verification ─────────────────────────────────────────────

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            InvalidVerificationCodeException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CodeRecentlySentException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            CodeRecentlySentException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Task ─────────────────────────────────────────────────────

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            TaskNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Category ─────────────────────────────────────────────────

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            CategoryNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CategoryNameExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            CategoryNameExistsException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Challenge ────────────────────────────────────────────────

    @ExceptionHandler(ChallengeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            ChallengeNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AlreadyJoinedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AlreadyJoinedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(NotJoinedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            NotJoinedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Post ─────────────────────────────────────────────────────

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            PostNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AlreadyLikedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AlreadyLikedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(NotLikedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            NotLikedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Comment ──────────────────────────────────────────────────

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            CommentNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Partner ──────────────────────────────────────────────────

    @ExceptionHandler(PartnerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            PartnerNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(PartnerNameExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            PartnerNameExistsException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Motivation ───────────────────────────────────────────────

    @ExceptionHandler(MotivationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            MotivationNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MotivationAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            MotivationAlreadyExistsException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Achievement ──────────────────────────────────────────────

    @ExceptionHandler(AchievementNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AchievementNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── AI ───────────────────────────────────────────────────────

    @ExceptionHandler(AiConfigNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AiConfigNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AiSessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AiSessionNotFoundException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AiServiceException ex, HttpServletRequest request) {
        log.error(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Access denied ────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    // ── Validation ───────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> ApiResponse.FieldError.builder()
                        .field(e.getField())
                        .message(e.getDefaultMessage())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError(
                        "Validation failed",
                        request.getRequestURI(),
                        fieldErrors));
    }

    // ── Fallback ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500)
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.getMessage(),
                        request.getRequestURI()));
    }
}