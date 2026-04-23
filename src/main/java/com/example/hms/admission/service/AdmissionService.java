package com.example.hms.admission.service;

import com.example.hms.admission.dto.AdmissionRequest;
import com.example.hms.admission.dto.AdmissionResponse;

public interface AdmissionService {
    AdmissionResponse admit(AdmissionRequest request);
    AdmissionResponse discharge(Long admissionId);
    java.util.List<AdmissionResponse> findByPatient(Long patientId);
}
