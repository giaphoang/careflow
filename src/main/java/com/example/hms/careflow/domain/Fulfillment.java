package com.example.hms.careflow.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cf_fulfillments")
public class Fulfillment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String idempotencyKey;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(unique = true) private SupplyRequest request;
    @Column(nullable = false) private Long itemId;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false, length = 100) private String fulfilledBy;
    @Column(nullable = false) private Instant fulfilledAt;
    protected Fulfillment() {}
    public Fulfillment(String key, SupplyRequest request, String actor, Instant now) {
        this.idempotencyKey = key; this.request = request; this.itemId = request.getItem().getId();
        this.quantity = request.getQuantity(); this.fulfilledBy = actor; this.fulfilledAt = now;
    }
    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public SupplyRequest getRequest() { return request; }
    public Long getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public String getFulfilledBy() { return fulfilledBy; }
    public Instant getFulfilledAt() { return fulfilledAt; }
}
