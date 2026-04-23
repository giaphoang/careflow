package com.example.hms.patient.service.impl;

import com.example.hms.common.exception.BusinessValidationException;
import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.patient.domain.Patient;
import com.example.hms.patient.dto.PatientRequest;
import com.example.hms.patient.dto.PatientResponse;
import com.example.hms.patient.repository.PatientRepository;
import com.example.hms.patient.service.PatientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public PatientResponse create(PatientRequest request) {
        patientRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new BusinessValidationException("Patient email already exists");
        });
        Patient patient = new Patient(request.fullName(), request.email(), request.phone(), request.dateOfBirth());
        return toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
        patientRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessValidationException("Patient email already exists");
                });
        patient.update(request.fullName(), request.email(), request.phone(), request.dateOfBirth());
        return toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        return toResponse(patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAll() {
        return patientRepository.findAll().stream().map(this::toResponse).toList();
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getDateOfBirth()
        );
    }
}
