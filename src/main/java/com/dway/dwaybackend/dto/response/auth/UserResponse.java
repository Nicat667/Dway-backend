package com.dway.dwaybackend.dto.response.auth;

import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String avatarUrl;
    private Plan plan;
    private Set<Role> roles;
    private int points;
    private int streak;
    private boolean isVerified;
}