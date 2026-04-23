package com.example.hms.billing.controller;

import com.example.hms.billing.dto.InvoiceRequest;
import com.example.hms.billing.dto.InvoiceResponse;
import com.example.hms.billing.service.BillingService;
import com.example.hms.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.success("Invoice created", billingService.createInvoice(request));
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<InvoiceResponse> markPaid(@PathVariable Long id) {
        return ApiResponse.success("Invoice paid", billingService.markPaid(id));
    }

    @GetMapping("/invoices/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<List<InvoiceResponse>> getByPatient(@PathVariable Long patientId) {
        return ApiResponse.success("Invoices fetched", billingService.getByPatient(patientId));
    }
}
