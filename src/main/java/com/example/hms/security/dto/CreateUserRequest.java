package com.example.hms.security.dto;

import com.example.hms.security.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
        @NotNull Role role
) {
}
