package com.example.hms.appointment.repository;

import com.example.hms.appointment.domain.Appointment;
import com.example.hms.appointment.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByDoctorIdAndAppointmentTimeAndStatus(Long doctorId, LocalDateTime appointmentTime, AppointmentStatus status);
    List<Appointment> findByPatientId(Long patientId);
}
