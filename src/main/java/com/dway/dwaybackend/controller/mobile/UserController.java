package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.user.ChangePasswordRequest;
import com.dway.dwaybackend.dto.request.user.UpdateProfileRequest;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.security.CurrentUser;
import com.dway.dwaybackend.service.mobile.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "User")
//@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/mobile/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get my profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(null, userService.getMyProfile(userId)));
    }

    @Operation(summary = "Update my profile (name, country, push token — all optional)")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@CurrentUser UUID userId, @RequestBody @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(userId, request)));
    }

    @Operation(summary = "Upload or replace my avatar")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(@CurrentUser UUID userId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Avatar updated", userService.uploadAvatar(userId, file)));
    }

    @Operation(summary = "Change my password — invalidates all sessions")
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@CurrentUser UUID userId, @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed. Please log in again.", null));
    }

    @Operation(summary = "Delete my account permanently")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser UUID userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }
}