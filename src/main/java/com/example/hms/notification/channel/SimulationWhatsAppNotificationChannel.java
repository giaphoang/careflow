package com.example.hms.notification.channel;

import com.example.hms.notification.model.NotificationMedium;
import com.example.hms.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification", name = "simulation-enabled", havingValue = "true", matchIfMissing = true)
public class SimulationWhatsAppNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(SimulationWhatsAppNotificationChannel.class);

    @Override
    public NotificationMedium medium() {
        return NotificationMedium.WHATSAPP;
    }

    @Override
    public void send(NotificationMessage message) {
        logger.info("event=NOTIFICATION_SIMULATED medium={} recipient={} type={} subject={} body={}",
                message.medium(),
                message.recipient(),
                message.eventType(),
                message.subject(),
                message.body());
    }
}
