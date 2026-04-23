package com.example.hms.appointment.service;

import com.example.hms.appointment.dto.AppointmentRequest;
import com.example.hms.appointment.dto.AppointmentResponse;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse book(AppointmentRequest request);
    AppointmentResponse complete(Long appointmentId);
    AppointmentResponse cancel(Long appointmentId);
    List<AppointmentResponse> getByPatient(Long patientId);
}
