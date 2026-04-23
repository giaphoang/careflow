package com.example.hms.doctor.controller;

import com.example.hms.common.api.ApiResponse;
import com.example.hms.doctor.dto.DoctorRequest;
import com.example.hms.doctor.dto.DoctorResponse;
import com.example.hms.doctor.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorResponse> create(@Valid @RequestBody DoctorRequest request) {
        return ApiResponse.success("Doctor created", doctorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorResponse> update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return ApiResponse.success("Doctor updated", doctorService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public ApiResponse<DoctorResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Doctor fetched", doctorService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public ApiResponse<List<DoctorResponse>> getAll() {
        return ApiResponse.success("Doctors fetched", doctorService.getAll());
    }
}
