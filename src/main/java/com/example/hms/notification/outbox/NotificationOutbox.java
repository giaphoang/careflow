package com.example.hms.notification.outbox;

import com.example.hms.notification.model.NotificationMedium;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_outbox", indexes = {
        @Index(name = "idx_notification_outbox_status_next_attempt", columnList = "status,next_attempt_at"),
        @Index(name = "idx_notification_outbox_created_at", columnList = "created_at")
})
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationMedium medium;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationOutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false, name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(NotificationMedium medium,
                              String recipient,
                              String subject,
                              String body,
                              String eventType,
                              int maxAttempts) {
        this.medium = medium;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.eventType = eventType;
        this.maxAttempts = maxAttempts;
        this.status = NotificationOutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
        if (status == null) {
            status = NotificationOutboxStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public NotificationMedium getMedium() {
        return medium;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getEventType() {
        return eventType;
    }

    public NotificationOutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void markSent() {
        this.status = NotificationOutboxStatus.SENT;
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String errorMessage, long baseDelaySeconds) {
        this.attempts += 1;
        this.lastError = abbreviate(errorMessage, 500);
        this.updatedAt = Instant.now();

        if (this.attempts >= this.maxAttempts) {
            this.status = NotificationOutboxStatus.FAILED;
            return;
        }

        this.status = NotificationOutboxStatus.PENDING;
        long multiplier = 1L << Math.min(this.attempts - 1, 6);
        long retryDelay = Math.max(1, baseDelaySeconds) * multiplier;
        this.nextAttemptAt = Instant.now().plusSeconds(retryDelay);
    }

    private String abbreviate(String value, int maxLen) {
        if (value == null) {
            return "unknown_error";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
