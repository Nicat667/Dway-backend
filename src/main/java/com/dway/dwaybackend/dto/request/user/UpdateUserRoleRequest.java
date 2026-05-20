package com.dway.dwaybackend.dto.request.user;

import com.dway.dwaybackend.entity.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Roles must not be null")
    private Set<Role> roles;
}