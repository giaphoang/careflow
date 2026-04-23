package com.example.hms.notification.channel;

import com.example.hms.notification.model.NotificationMedium;
import com.example.hms.notification.model.NotificationMessage;

public interface NotificationChannel {
    NotificationMedium medium();

    void send(NotificationMessage message);
}
