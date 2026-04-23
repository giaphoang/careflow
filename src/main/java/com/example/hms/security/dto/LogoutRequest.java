package com.example.hms.security.dto;

public record LogoutRequest(
        String refreshToken
) {
}
