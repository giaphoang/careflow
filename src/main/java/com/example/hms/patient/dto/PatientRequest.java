package com.example.hms.patient.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank String phone,
        @NotNull @Past LocalDate dateOfBirth
) {}
