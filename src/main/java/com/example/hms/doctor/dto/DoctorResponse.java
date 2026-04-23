package com.example.hms.doctor.dto;

public record DoctorResponse(
        Long id,
        String fullName,
        String specialization,
        String email
) {}
