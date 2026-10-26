package com.example.hms.careflow.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cf_requests", indexes = @Index(name = "cf_request_owner", columnList = "requestedBy"))
public class SupplyRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private InventoryItem item;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false, length = 100) private String requestedBy;
    @Column(nullable = false, length = 100) private String department;
    @Column(nullable = false, length = 500) private String purpose;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RequestState state;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant reservedUntil;

    protected SupplyRequest() {}
    public SupplyRequest(InventoryItem item, int quantity, String requestedBy, String department, String purpose, Instant now) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.item = item; this.quantity = quantity; this.requestedBy = requestedBy;
        this.department = department; this.purpose = purpose; this.state = RequestState.REQUESTED;
        this.createdAt = now; this.updatedAt = now;
    }
    public void transition(RequestState next, Instant now) { state = next; updatedAt = now; }
    public void setReservedUntil(Instant until) { reservedUntil = until; }
    public Long getId() { return id; }
    public InventoryItem getItem() { return item; }
    public int getQuantity() { return quantity; }
    public String getRequestedBy() { return requestedBy; }
    public String getDepartment() { return department; }
    public String getPurpose() { return purpose; }
    public RequestState getState() { return state; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getReservedUntil() { return reservedUntil; }
}
