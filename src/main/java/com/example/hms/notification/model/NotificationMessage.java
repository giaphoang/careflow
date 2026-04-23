package com.example.hms.notification.model;

public record NotificationMessage(
        NotificationMedium medium,
        String recipient,
        String subject,
        String body,
        String eventType
) {
}
