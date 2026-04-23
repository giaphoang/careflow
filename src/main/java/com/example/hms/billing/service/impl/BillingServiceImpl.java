package com.example.hms.billing.service.impl;

import com.example.hms.billing.domain.Invoice;
import com.example.hms.billing.domain.InvoiceStatus;
import com.example.hms.billing.dto.InvoiceRequest;
import com.example.hms.billing.dto.InvoiceResponse;
import com.example.hms.billing.repository.InvoiceRepository;
import com.example.hms.billing.service.BillingService;
import com.example.hms.common.exception.BusinessValidationException;
import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.patient.domain.Patient;
import com.example.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PatientRepository patientRepository;

    public BillingServiceImpl(InvoiceRepository invoiceRepository, PatientRepository patientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        Invoice invoice = new Invoice(patient, request.amount(), request.description());
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public InvoiceResponse markPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessValidationException("Invoice already paid");
        }
        invoice.markPaid();
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByPatient(Long patientId) {
        return invoiceRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getPatient().getId(),
                invoice.getPatient().getFullName(),
                invoice.getAmount(),
                invoice.getIssuedOn(),
                invoice.getStatus(),
                invoice.getDescription()
        );
    }
}
