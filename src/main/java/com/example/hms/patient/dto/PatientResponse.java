package com.example.hms.patient.dto;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth
) {}
