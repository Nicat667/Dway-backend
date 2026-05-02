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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Auth ─────────────────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(UserNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(EmailAlreadyExistsException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(InvalidCredentialsException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(EmailNotVerifiedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(UserBannedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handle(InvalidTokenException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handle(InvalidRefreshTokenException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Verification ─────────────────────────────────────────────

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ApiResponse<Void>> handle(InvalidVerificationCodeException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CodeRecentlySentException.class)
    public ResponseEntity<ApiResponse<Void>> handle(CodeRecentlySentException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Task ─────────────────────────────────────────────────────

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(TaskNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Category ─────────────────────────────────────────────────

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(CategoryNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CategoryNameExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(CategoryNameExistsException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Challenge ────────────────────────────────────────────────

    @ExceptionHandler(ChallengeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ChallengeNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AlreadyJoinedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AlreadyJoinedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NotJoinedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(NotJoinedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Post ─────────────────────────────────────────────────────

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(PostNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AlreadyLikedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AlreadyLikedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NotLikedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(NotLikedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Comment ──────────────────────────────────────────────────

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(CommentNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Partner ──────────────────────────────────────────────────

    @ExceptionHandler(PartnerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(PartnerNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PartnerNameExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(PartnerNameExistsException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Motivation ───────────────────────────────────────────────

    @ExceptionHandler(MotivationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MotivationNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MotivationAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MotivationAlreadyExistsException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Achievement ──────────────────────────────────────────────

    @ExceptionHandler(AchievementNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AchievementNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── AI ───────────────────────────────────────────────────────

    @ExceptionHandler(AiConfigNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AiConfigNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AiSessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AiSessionNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AiServiceException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Validation ───────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AccessDeniedException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Fallback ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}