package com.example.hms.security.dto;

import com.example.hms.security.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role,
        String fullName,
        String specialization,
        @Email String email
) {
}
