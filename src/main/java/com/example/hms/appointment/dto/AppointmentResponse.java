package com.example.hms.appointment.dto;

import com.example.hms.appointment.domain.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        LocalDateTime appointmentTime,
        AppointmentStatus status,
        String reason
) {}
