package com.example.hms.admission.dto;

import com.example.hms.admission.domain.AdmissionStatus;

import java.time.LocalDate;

public record AdmissionResponse(
        Long id,
        Long patientId,
        String patientName,
        String ward,
        LocalDate admittedOn,
        LocalDate dischargedOn,
        AdmissionStatus status
) {}
