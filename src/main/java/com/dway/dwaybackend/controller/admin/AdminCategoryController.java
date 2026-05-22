package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.category.CreateCategoryRequest;
import com.dway.dwaybackend.dto.request.category.UpdateCategoryRequest;
import com.dway.dwaybackend.dto.response.category.CategoryResponse;
import com.dway.dwaybackend.service.admin.AdminCategoryService;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Categories")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @Operation(summary = "Get all default system categories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(null, adminCategoryService.getAllDefaultCategories()));
    }

    @Operation(summary = "Create a default system category (visible to all users)")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createDefaultCategory(@RequestBody @Valid CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category created", adminCategoryService.createDefaultCategory(request)));
    }

    @Operation(summary = "Update any category (default or user-created)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateDefaultCategory(@PathVariable UUID id, @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", adminCategoryService.updateDefaultCategory(id, request)));
    }

    @Operation(summary = "Delete any category (default or user-created)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDefaultCategory(@PathVariable UUID id) {
        adminCategoryService.deleteDefaultCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}