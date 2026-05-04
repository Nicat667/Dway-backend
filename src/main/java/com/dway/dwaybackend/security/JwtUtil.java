package com.dway.dwaybackend.security;

import com.dway.dwaybackend.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry:900}")
    private long accessTokenExpiry;

    @Value("${app.jwt.refresh-token-expiry:2592000}")
    private long refreshTokenExpiry;

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Accepts Set<Role> — user can have multiple roles
    // Stored in token as: ["USER", "ADMIN"]
    public String generateAccessToken(UUID userId, Set<Role> roles) {
        List<String> roleNames = roles.stream()
                .map(Role::name)
                .toList();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roleNames)   // "roles" plural — List not single value
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry * 1000))
                .signWith(getSignKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiry * 1000))
                .signWith(getSignKey())
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    // Extracts List<String> from token, converts each back to Role enum
    public Set<Role> extractRoles(String token) {
        List<String> roleNames = extractClaims(token).get("roles", List.class);
        return roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}