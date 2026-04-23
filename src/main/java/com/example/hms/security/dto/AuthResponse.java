package com.example.hms.security.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String username,
        String role
) {
}
