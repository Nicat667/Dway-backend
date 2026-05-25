package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.challenge.CreateChallengeRequest;
import com.dway.dwaybackend.dto.request.challenge.UpdateChallengeRequest;
import com.dway.dwaybackend.dto.response.challenge.AdminChallengeResponse;
import com.dway.dwaybackend.service.admin.AdminChallengeService;
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

@Tag(name = "Admin — Challenges")
@RestController
@RequestMapping("/api/v1/admin/challenges")
@RequiredArgsConstructor
public class AdminChallengeController {

    private final AdminChallengeService adminChallengeService;

    @Operation(summary = "Get all challenges paginated. Optional isActive filter")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminChallengeResponse>>> getAllChallenges(@RequestParam(required = false) Boolean isActive, @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, adminChallengeService.getAllChallenges(isActive, pageable)));
    }

    @Operation(summary = "Get a single challenge by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminChallengeResponse>> getChallengeById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(null, adminChallengeService.getChallengeById(id)));
    }

    @Operation(summary = "Create a new challenge")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminChallengeResponse>> createChallenge(@RequestBody @Valid CreateChallengeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Challenge created", adminChallengeService.createChallenge(request)));
    }

    @Operation(summary = "Update challenge fields (PATCH semantics, only non-null fields applied)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminChallengeResponse>> updateChallenge(@PathVariable UUID id, @RequestBody @Valid UpdateChallengeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Challenge updated", adminChallengeService.updateChallenge(id, request)));
    }

    @Operation(summary = "Toggle isActive on/off")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AdminChallengeResponse>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Challenge status toggled", adminChallengeService.toggleActive(id)));
    }

    @Operation(summary = "Delete a challenge permanently")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(@PathVariable UUID id) {
        adminChallengeService.deleteChallenge(id);
        return ResponseEntity.ok(ApiResponse.success("Challenge deleted", null));
    }
}