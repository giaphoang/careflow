package com.example.hms.appointment.controller;

import com.example.hms.appointment.dto.AppointmentRequest;
import com.example.hms.appointment.dto.AppointmentResponse;
import com.example.hms.appointment.service.AppointmentService;
import com.example.hms.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        return ApiResponse.success("Appointment booked", appointmentService.book(request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ApiResponse<AppointmentResponse> complete(@PathVariable Long id) {
        return ApiResponse.success("Appointment completed", appointmentService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Appointment cancelled", appointmentService.cancel(id));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public ApiResponse<List<AppointmentResponse>> getByPatient(@PathVariable Long patientId) {
        return ApiResponse.success("Appointments fetched", appointmentService.getByPatient(patientId));
    }
}
