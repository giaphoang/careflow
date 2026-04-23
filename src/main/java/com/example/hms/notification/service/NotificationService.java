package com.example.hms.notification.service;

import java.time.LocalDateTime;

public interface NotificationService {
    void sendAppointmentBooked(String patientName,
                               String patientPhone,
                               String doctorName,
                               LocalDateTime appointmentTime,
                               String reason);

    void sendAppointmentCancelled(String patientName,
                                  String patientPhone,
                                  String doctorName,
                                  LocalDateTime appointmentTime);
}
