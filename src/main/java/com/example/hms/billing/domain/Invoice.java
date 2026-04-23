package com.example.hms.billing.domain;

import com.example.hms.patient.domain.Patient;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate issuedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(nullable = false)
    private String description;

    protected Invoice() {}

    public Invoice(Patient patient, BigDecimal amount, String description) {
        this.patient = patient;
        this.amount = amount;
        this.description = description;
        this.issuedOn = LocalDate.now();
        this.status = InvoiceStatus.UNPAID;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public InvoiceStatus getStatus() { return status; }
    public String getDescription() { return description; }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
    }
}
