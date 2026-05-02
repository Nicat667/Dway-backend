package com.dway.dwaybackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    USER_NOT_FOUND              (HttpStatus.NOT_FOUND,              "User not found"),
    EMAIL_ALREADY_EXISTS        (HttpStatus.CONFLICT,               "Email is already registered"),
    INVALID_CREDENTIALS         (HttpStatus.UNAUTHORIZED,           "Invalid email or password"),
    EMAIL_NOT_VERIFIED          (HttpStatus.FORBIDDEN,              "Please verify your email before logging in"),
    USER_BANNED                 (HttpStatus.FORBIDDEN,              "Your account has been banned"),
    INVALID_TOKEN               (HttpStatus.UNAUTHORIZED,           "Invalid or expired token"),
    INVALID_REFRESH_TOKEN       (HttpStatus.UNAUTHORIZED,           "Invalid or expired refresh token"),

    // Email verification
    INVALID_VERIFICATION_CODE   (HttpStatus.BAD_REQUEST,            "Invalid or expired verification code"),
    CODE_ALREADY_USED           (HttpStatus.BAD_REQUEST,            "This code has already been used"),
    CODE_RECENTLY_SENT          (HttpStatus.TOO_MANY_REQUESTS,      "A code was recently sent, please wait before requesting another"),

    // Task
    TASK_NOT_FOUND              (HttpStatus.NOT_FOUND,              "Task not found"),
    TASK_ACCESS_DENIED          (HttpStatus.FORBIDDEN,              "You do not have access to this task"),

    // Category
    CATEGORY_NOT_FOUND          (HttpStatus.NOT_FOUND,              "Category not found"),
    CATEGORY_ACCESS_DENIED      (HttpStatus.FORBIDDEN,              "You do not have access to this category"),
    CATEGORY_NAME_EXISTS        (HttpStatus.CONFLICT,               "You already have a category with this name"),

    // Challenge
    CHALLENGE_NOT_FOUND         (HttpStatus.NOT_FOUND,              "Challenge not found"),
    ALREADY_JOINED              (HttpStatus.CONFLICT,               "You have already joined this challenge"),
    NOT_JOINED                  (HttpStatus.BAD_REQUEST,            "You have not joined this challenge"),

    // Post
    POST_NOT_FOUND              (HttpStatus.NOT_FOUND,              "Post not found"),
    POST_ACCESS_DENIED          (HttpStatus.FORBIDDEN,              "You do not have access to this post"),
    ALREADY_LIKED               (HttpStatus.CONFLICT,               "You have already liked this post"),
    NOT_LIKED                   (HttpStatus.BAD_REQUEST,            "You have not liked this post"),

    // Comment
    COMMENT_NOT_FOUND           (HttpStatus.NOT_FOUND,              "Comment not found"),
    COMMENT_ACCESS_DENIED       (HttpStatus.FORBIDDEN,              "You do not have access to this comment"),

    // Partner
    PARTNER_NOT_FOUND           (HttpStatus.NOT_FOUND,              "Partner not found"),
    PARTNER_NAME_EXISTS         (HttpStatus.CONFLICT,               "A partner with this name already exists"),

    // Motivation
    MOTIVATION_NOT_FOUND        (HttpStatus.NOT_FOUND,              "Motivation not found"),
    MOTIVATION_ALREADY_EXISTS   (HttpStatus.CONFLICT,               "A quote for this language and date already exists"),

    // Achievement
    ACHIEVEMENT_NOT_FOUND       (HttpStatus.NOT_FOUND,              "Achievement not found"),

    // AI
    AI_CONFIG_NOT_FOUND         (HttpStatus.NOT_FOUND,              "AI configuration not found"),
    AI_SESSION_NOT_FOUND        (HttpStatus.NOT_FOUND,              "AI chat session not found"),
    AI_SERVICE_ERROR            (HttpStatus.INTERNAL_SERVER_ERROR,  "AI service is currently unavailable"),

    // General
    ACCESS_DENIED               (HttpStatus.FORBIDDEN,              "You do not have permission to perform this action"),
    INTERNAL_ERROR              (HttpStatus.INTERNAL_SERVER_ERROR,  "An unexpected error occurred");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}