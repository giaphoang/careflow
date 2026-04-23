package com.example.hms.patient.service;

import com.example.hms.patient.dto.PatientRequest;
import com.example.hms.patient.dto.PatientResponse;

import java.util.List;

public interface PatientService {
    PatientResponse create(PatientRequest request);
    PatientResponse update(Long id, PatientRequest request);
    PatientResponse getById(Long id);
    List<PatientResponse> getAll();
}
