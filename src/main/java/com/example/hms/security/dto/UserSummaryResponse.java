package com.example.hms.security.dto;

import com.example.hms.security.model.Role;

public record UserSummaryResponse(
        Long id,
        String username,
        Role role
) {
}
