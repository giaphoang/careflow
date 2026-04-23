package com.example.hms.doctor.service.impl;

import com.example.hms.common.exception.BusinessValidationException;
import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.doctor.domain.Doctor;
import com.example.hms.doctor.dto.DoctorRequest;
import com.example.hms.doctor.dto.DoctorResponse;
import com.example.hms.doctor.repository.DoctorRepository;
import com.example.hms.doctor.service.DoctorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    public DoctorResponse create(DoctorRequest request) {
        doctorRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new BusinessValidationException("Doctor email already exists");
        });
        Doctor doctor = new Doctor(request.fullName(), request.specialization(), request.email());
        return toResponse(doctorRepository.save(doctor));
    }

    @Override
    public DoctorResponse update(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
        doctorRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessValidationException("Doctor email already exists");
                });
        doctor.update(request.fullName(), request.specialization(), request.email());
        return toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getById(Long id) {
        return toResponse(doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll().stream().map(this::toResponse).toList();
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getSpecialization(),
                doctor.getEmail()
        );
    }
}
