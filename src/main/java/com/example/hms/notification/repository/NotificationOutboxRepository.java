package com.example.hms.notification.repository;

import com.example.hms.notification.outbox.NotificationOutbox;
import com.example.hms.notification.outbox.NotificationOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    List<NotificationOutbox> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationOutboxStatus status,
            Instant nextAttemptAt,
            Pageable pageable
    );
}
