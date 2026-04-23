package com.example.hms.notification.service;

import com.example.hms.notification.model.NotificationMessage;
import com.example.hms.notification.outbox.NotificationOutbox;

import java.util.List;

public interface NotificationOutboxService {
    void enqueue(NotificationMessage message);

    List<NotificationOutbox> fetchDueBatch();

    void markSent(Long outboxId);

    void markFailed(Long outboxId, String errorMessage);
}
