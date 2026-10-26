package com.example.hms.careflow.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cf_audit", indexes = @Index(name = "cf_audit_resource", columnList = "resourceType,resourceId"))
public class AuditEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, updatable = false, length = 100) private String actor;
    @Column(nullable = false, updatable = false, length = 40) private String action;
    @Column(nullable = false, updatable = false, length = 20) private String resourceType;
    @Column(nullable = false, updatable = false) private Long resourceId;
    @Column(updatable = false, length = 100) private String previousState;
    @Column(nullable = false, updatable = false, length = 100) private String newState;
    @Column(nullable = false, updatable = false, length = 500) private String detail;
    @Column(nullable = false, updatable = false) private Instant occurredAt;
    protected AuditEntry() {}
    public AuditEntry(String actor, String action, String resourceType, Long resourceId, String previousState,
                      String newState, String detail, Instant now) {
        this.actor = actor; this.action = action; this.resourceType = resourceType; this.resourceId = resourceId;
        this.previousState = previousState; this.newState = newState; this.detail = detail; this.occurredAt = now;
    }
    public Long getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
