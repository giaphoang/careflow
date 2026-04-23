package com.example.hms.admission.service.impl;

import com.example.hms.admission.domain.Admission;
import com.example.hms.admission.domain.AdmissionStatus;
import com.example.hms.admission.dto.AdmissionRequest;
import com.example.hms.admission.dto.AdmissionResponse;
import com.example.hms.admission.repository.AdmissionRepository;
import com.example.hms.admission.service.AdmissionService;
import com.example.hms.common.exception.BusinessValidationException;
import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.patient.domain.Patient;
import com.example.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class AdmissionServiceImpl implements AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;

    public AdmissionServiceImpl(AdmissionRepository admissionRepository, PatientRepository patientRepository) {
        this.admissionRepository = admissionRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public AdmissionResponse admit(AdmissionRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));

        boolean alreadyAdmitted = admissionRepository.existsByPatientIdAndStatus(patient.getId(), AdmissionStatus.ADMITTED);
        if (alreadyAdmitted) {
            throw new BusinessValidationException("Patient already admitted");
        }

        Admission admission = new Admission(patient, request.ward(), request.admittedOn());
        return toResponse(admissionRepository.save(admission));
    }

    @Override
    public AdmissionResponse discharge(Long admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + admissionId));
        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new BusinessValidationException("Admission already discharged");
        }
        admission.discharge(LocalDate.now());
        return toResponse(admission);
    }

    @Override
    public java.util.List<AdmissionResponse> findByPatient(Long patientId) {
        return admissionRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AdmissionResponse toResponse(Admission admission) {
        return new AdmissionResponse(
                admission.getId(),
                admission.getPatient().getId(),
                admission.getPatient().getFullName(),
                admission.getWard(),
                admission.getAdmittedOn(),
                admission.getDischargedOn(),
                admission.getStatus()
        );
    }
}
