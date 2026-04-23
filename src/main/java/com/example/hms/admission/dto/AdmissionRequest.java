package com.example.hms.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdmissionRequest(
        @NotNull Long patientId,
        @NotBlank String ward,
        @NotNull LocalDate admittedOn
) {}
