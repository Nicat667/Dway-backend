package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.motivation.CreateMotivationRequest;
import com.dway.dwaybackend.dto.request.motivation.UpdateMotivationRequest;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.service.admin.AdminMotivationService;
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

@Tag(name = "Admin — Motivations")
@RestController
@RequestMapping("/api/v1/admin/motivations")
@RequiredArgsConstructor
public class AdminMotivationController {

    private final AdminMotivationService adminMotivationService;

    @Operation(summary = "Get all motivations paginated, ordered by creation date descending")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MotivationResponse>>> getAllMotivations(@ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, adminMotivationService.getAllMotivations(pageable)));
    }

    @Operation(summary = "Get a single motivation by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MotivationResponse>> getMotivationById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(null, adminMotivationService.getMotivationById(id)));
    }

    @Operation(summary = "Upload a new motivation quote — system picks randomly which to show each day")
    @PostMapping
    public ResponseEntity<ApiResponse<MotivationResponse>> createMotivation(@RequestBody @Valid CreateMotivationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Motivation created", adminMotivationService.createMotivation(request)));
    }

    @Operation(summary = "Update a motivation — PATCH semantics, only non-null fields applied")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MotivationResponse>> updateMotivation(@PathVariable UUID id, @RequestBody UpdateMotivationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Motivation updated", adminMotivationService.updateMotivation(id, request)));
    }

    @Operation(summary = "Delete a motivation")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMotivation(@PathVariable UUID id) {
        adminMotivationService.deleteMotivation(id);
        return ResponseEntity.ok(ApiResponse.success("Motivation deleted", null));
    }
}