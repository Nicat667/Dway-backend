package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.achievement.CreateAchievementRequest;
import com.dway.dwaybackend.dto.request.achievement.UpdateAchievementRequest;
import com.dway.dwaybackend.dto.response.achievement.AchievementResponse;
import com.dway.dwaybackend.service.admin.AdminAchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin — Achievements")
@RestController
@RequestMapping("/api/v1/admin/achievements")
@RequiredArgsConstructor
public class AdminAchievementController {

    private final AdminAchievementService adminAchievementService;

    @Operation(summary = "Get all achievements paginated. Optional isActive filter")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AchievementResponse>>> getAllAchievements(@RequestParam(required = false) Boolean isActive, @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, adminAchievementService.getAllAchievements(isActive, pageable)));
    }

    @Operation(summary = "Get a single achievement by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AchievementResponse>> getAchievementById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(null, adminAchievementService.getAchievementById(id)));
    }

    @Operation(summary = "Create a new achievement — starts inactive by default")
    @PostMapping
    public ResponseEntity<ApiResponse<AchievementResponse>> createAchievement(@RequestBody @Valid CreateAchievementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Achievement created", adminAchievementService.createAchievement(request)));
    }

    @Operation(summary = "Update achievement fields (PATCH semantics, only non-null fields applied)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AchievementResponse>> updateAchievement(@PathVariable UUID id, @RequestBody @Valid UpdateAchievementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Achievement updated", adminAchievementService.updateAchievement(id, request)));
    }

    @Operation(summary = "Toggle isActive on/off — makes achievement visible or hidden from users")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AchievementResponse>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Achievement status toggled", adminAchievementService.toggleActive(id)));
    }

    @Operation(summary = "Delete an achievement permanently")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAchievement(@PathVariable UUID id) {
        adminAchievementService.deleteAchievement(id);
        return ResponseEntity.ok(ApiResponse.success("Achievement deleted", null));
    }
}