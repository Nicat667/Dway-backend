package com.dway.dwaybackend.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponse {
    private String accessToken;
}