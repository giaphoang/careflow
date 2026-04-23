package com.example.hms.doctor.service;

import com.example.hms.doctor.dto.DoctorRequest;
import com.example.hms.doctor.dto.DoctorResponse;

import java.util.List;

public interface DoctorService {
    DoctorResponse create(DoctorRequest request);
    DoctorResponse update(Long id, DoctorRequest request);
    DoctorResponse getById(Long id);
    List<DoctorResponse> getAll();
}
