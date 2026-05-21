package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.user.UpdateUserRoleRequest;
import com.dway.dwaybackend.dto.response.user.AdminUserResponse;
import com.dway.dwaybackend.security.CurrentUser;
import com.dway.dwaybackend.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Tag(name = "Admin — Users")
//@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Get all users (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getAllUsers(@ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, adminUserService.getAllUsers(pageable)));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(null, adminUserService.getUserById(userId)));
    }

    @Operation(summary = "Ban a user — immediately invalidates all their sessions")
    @PostMapping("/{userId}/ban")
    public ResponseEntity<ApiResponse<AdminUserResponse>> banUser(@PathVariable UUID userId, @CurrentUser UUID adminId) {
        return ResponseEntity.ok(ApiResponse.success("User banned", adminUserService.banUser(userId, adminId)));
    }

    @Operation(summary = "Unban a user")
    @PostMapping("/{userId}/unban")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unbanUser(@PathVariable UUID userId, @CurrentUser UUID adminId) {
        return ResponseEntity.ok(ApiResponse.success("User unbanned", adminUserService.unbanUser(userId, adminId)));
    }

    @Operation(summary = "Update user roles")
    @PutMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRoles(@PathVariable UUID userId, @RequestBody @Valid UpdateUserRoleRequest request, @CurrentUser UUID adminId) {
        return ResponseEntity.ok(ApiResponse.success("Roles updated", adminUserService.updateUserRoles(userId, request, adminId)));
    }

    @Operation(summary = "Delete a user permanently")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId, @CurrentUser UUID adminId) {
        adminUserService.deleteUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}