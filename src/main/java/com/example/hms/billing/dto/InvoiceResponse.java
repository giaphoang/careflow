package com.example.hms.billing.dto;

import com.example.hms.billing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id,
        Long patientId,
        String patientName,
        BigDecimal amount,
        LocalDate issuedOn,
        InvoiceStatus status,
        String description
) {}
