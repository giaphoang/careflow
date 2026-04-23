package com.example.hms.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceRequest(
        @NotNull Long patientId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String description
) {}
