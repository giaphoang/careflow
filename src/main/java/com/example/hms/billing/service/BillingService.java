package com.example.hms.billing.service;

import com.example.hms.billing.dto.InvoiceRequest;
import com.example.hms.billing.dto.InvoiceResponse;

import java.util.List;

public interface BillingService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    InvoiceResponse markPaid(Long invoiceId);
    List<InvoiceResponse> getByPatient(Long patientId);
}
