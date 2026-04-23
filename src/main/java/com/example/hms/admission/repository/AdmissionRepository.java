package com.example.hms.admission.repository;

import com.example.hms.admission.domain.Admission;
import com.example.hms.admission.domain.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    boolean existsByPatientIdAndStatus(Long patientId, AdmissionStatus status);
    java.util.List<Admission> findByPatientId(Long patientId);
}
