package com.example.hms.notification.service.impl;

import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.notification.model.NotificationMessage;
import com.example.hms.notification.outbox.NotificationOutbox;
import com.example.hms.notification.outbox.NotificationOutboxStatus;
import com.example.hms.notification.repository.NotificationOutboxRepository;
import com.example.hms.notification.service.NotificationOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;

    @Value("${notification.outbox.batch-size:50}")
    private int batchSize;

    @Value("${notification.outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${notification.outbox.base-retry-seconds:30}")
    private long baseRetrySeconds;

    public NotificationOutboxServiceImpl(NotificationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void enqueue(NotificationMessage message) {
        NotificationOutbox outbox = new NotificationOutbox(
                message.medium(),
                message.recipient(),
                message.subject(),
                message.body(),
                message.eventType(),
                Math.max(1, maxAttempts)
        );
        outboxRepository.save(outbox);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationOutbox> fetchDueBatch() {
        return outboxRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                NotificationOutboxStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, Math.max(1, batchSize))
        );
    }

    @Override
    public void markSent(Long outboxId) {
        NotificationOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox message not found: " + outboxId));
        outbox.markSent();
    }

    @Override
    public void markFailed(Long outboxId, String errorMessage) {
        NotificationOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox message not found: " + outboxId));
        outbox.markFailed(errorMessage, baseRetrySeconds);
    }
}
