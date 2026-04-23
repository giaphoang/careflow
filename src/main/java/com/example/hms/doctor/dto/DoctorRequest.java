package com.example.hms.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DoctorRequest(
        @NotBlank String fullName,
        @NotBlank String specialization,
        @Email @NotBlank String email
) {}
