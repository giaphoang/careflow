package com.example.hms.notification.service.impl;

import com.example.hms.notification.model.NotificationMessage;
import com.example.hms.notification.outbox.NotificationOutbox;
import com.example.hms.notification.service.NotificationDispatcher;
import com.example.hms.notification.service.NotificationOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationOutboxProcessor {

    private static final Logger logger = LoggerFactory.getLogger(NotificationOutboxProcessor.class);

    private final NotificationOutboxService outboxService;
    private final NotificationDispatcher dispatcher;

    public NotificationOutboxProcessor(NotificationOutboxService outboxService,
                                       NotificationDispatcher dispatcher) {
        this.outboxService = outboxService;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${notification.outbox.poll-ms:5000}")
    public void processDueNotifications() {
        List<NotificationOutbox> due = outboxService.fetchDueBatch();
        for (NotificationOutbox outbox : due) {
            try {
                dispatcher.dispatch(toMessage(outbox));
                outboxService.markSent(outbox.getId());
            } catch (RuntimeException ex) {
                outboxService.markFailed(outbox.getId(), ex.getMessage());
                logger.warn("Outbox delivery failed: id={}, medium={}, recipient={}, eventType={}, reason={}",
                        outbox.getId(),
                        outbox.getMedium(),
                        outbox.getRecipient(),
                        outbox.getEventType(),
                        ex.getMessage());
            }
        }
    }

    private NotificationMessage toMessage(NotificationOutbox outbox) {
        return new NotificationMessage(
                outbox.getMedium(),
                outbox.getRecipient(),
                outbox.getSubject(),
                outbox.getBody(),
                outbox.getEventType()
        );
    }
}
