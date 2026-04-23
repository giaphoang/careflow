package com.example.hms.patient.controller;

import com.example.hms.common.api.ApiResponse;
import com.example.hms.patient.dto.PatientRequest;
import com.example.hms.patient.dto.PatientResponse;
import com.example.hms.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        return ApiResponse.success("Patient created", patientService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return ApiResponse.success("Patient updated", patientService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public ApiResponse<PatientResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Patient fetched", patientService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public ApiResponse<List<PatientResponse>> getAll() {
        return ApiResponse.success("Patients fetched", patientService.getAll());
    }
}
