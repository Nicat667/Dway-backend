package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.category.CreateCategoryRequest;
import com.dway.dwaybackend.dto.request.category.UpdateCategoryRequest;
import com.dway.dwaybackend.dto.response.category.CategoryResponse;
import com.dway.dwaybackend.security.CurrentUser;
import com.dway.dwaybackend.service.mobile.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/v1/mobile/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get all categories (default system categories + own custom categories)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(null, categoryService.getAllCategories(userId)));
    }

    @Operation(summary = "Create a new custom category")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@CurrentUser UUID userId, @RequestBody @Valid CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category created", categoryService.createCategory(userId, request)));
    }

    @Operation(summary = "Update a custom category (PATCH semantics — only non-null fields applied)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.updateCategory(userId, id, request)));
    }

    @Operation(summary = "Delete a custom category")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@CurrentUser UUID userId, @PathVariable UUID id) {
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}