package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.response.achievement.AchievementStatusResponse;
import com.dway.dwaybackend.security.CurrentUser;
import com.dway.dwaybackend.service.mobile.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Mobile — Achievements")
@RestController
@RequestMapping("/api/v1/mobile/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @Operation(summary = "List all active achievements with earned status for the current user")
    @GetMapping
    @PreAuthorize("#userId == authentication.principal")
    public ResponseEntity<ApiResponse<List<AchievementStatusResponse>>> getAllAchievements(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(null, achievementService.getAllAchievements(userId)));
    }

    @Operation(summary = "List only the achievements the current user has earned, newest first")
    @GetMapping("/me")
    @PreAuthorize("#userId == authentication.principal")
    public ResponseEntity<ApiResponse<List<AchievementStatusResponse>>> getMyAchievements(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(null, achievementService.getMyAchievements(userId)));
    }
}