package com.dway.dwaybackend.dto.response.user;

import com.dway.dwaybackend.entity.enums.Country;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
public class AdminUserResponse {
    private UUID id;
    private String name;
    private String surname;
    private String email;
    private String avatarUrl;
    private Country country;
    private Plan plan;
    private Set<Role> roles;
    private int points;
    private int streak;
    private boolean isVerified;
    private boolean isBanned;
    private LocalDateTime bannedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}