package com.dway.dwaybackend.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Optional — stored with refresh token to identify which device logged in
    // Mobile app sends "iPhone 14 Pro" or "Samsung Galaxy S23"
    // Allows showing active sessions to user later: "Logged in from iPhone 14 Pro"
    private String deviceInfo;
}