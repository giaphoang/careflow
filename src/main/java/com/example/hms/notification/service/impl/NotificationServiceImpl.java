package com.example.hms.notification.service.impl;

import com.example.hms.notification.model.NotificationMedium;
import com.example.hms.notification.model.NotificationMessage;
import com.example.hms.notification.service.NotificationService;
import com.example.hms.notification.service.NotificationOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationOutboxService outboxService;

    public NotificationServiceImpl(NotificationOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    public void sendAppointmentBooked(String patientName,
                                      String patientPhone,
                                      String doctorName,
                                      LocalDateTime appointmentTime,
                                      String reason) {
        String patientBody = "Dear " + patientName + ", your appointment with Dr. " + doctorName +
                " is booked for " + appointmentTime + ". Reason: " + reason + ".";
        sendWhatsApp(patientPhone, patientBody, "APPOINTMENT_BOOKED_PATIENT_WHATSAPP");
    }

    @Override
    public void sendAppointmentCancelled(String patientName,
                                         String patientPhone,
                                         String doctorName,
                                         LocalDateTime appointmentTime) {
        String patientBody = "Dear " + patientName + ", your appointment with Dr. " + doctorName +
                " on " + appointmentTime + " has been cancelled.";
        sendWhatsApp(patientPhone, patientBody, "APPOINTMENT_CANCELLED_PATIENT_WHATSAPP");
    }

    private void sendWhatsApp(String recipient, String body, String eventType) {
        sendByMedium(NotificationMedium.WHATSAPP, recipient, "HMS Notification", body, eventType);
    }

    private void sendByMedium(NotificationMedium medium, String recipient, String subject, String body, String eventType) {
        if (recipient == null || recipient.isBlank()) {
            logger.warn("Notification skipped: medium={}, eventType={}, reason=recipient_missing", medium, eventType);
            return;
        }
        NotificationMessage message = new NotificationMessage(medium, recipient, subject, body, eventType);
        outboxService.enqueue(message);
    }
}
