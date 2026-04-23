package com.example.hms.admission.controller;

import com.example.hms.admission.dto.AdmissionRequest;
import com.example.hms.admission.dto.AdmissionResponse;
import com.example.hms.admission.service.AdmissionService;
import com.example.hms.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionController {

    private final AdmissionService admissionService;

    public AdmissionController(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<AdmissionResponse> admit(@Valid @RequestBody AdmissionRequest request) {
        return ApiResponse.success("Patient admitted", admissionService.admit(request));
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<AdmissionResponse> discharge(@PathVariable Long id) {
        return ApiResponse.success("Patient discharged", admissionService.discharge(id));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<java.util.List<AdmissionResponse>> getByPatient(@PathVariable Long patientId) {
        return ApiResponse.success("Admissions fetched", admissionService.findByPatient(patientId));
    }
}
