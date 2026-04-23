package com.example.hms.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        @NotNull @Future LocalDateTime appointmentTime,
        @NotBlank String reason
) {}
