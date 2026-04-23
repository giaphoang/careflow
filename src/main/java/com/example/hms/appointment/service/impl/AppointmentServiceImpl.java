package com.example.hms.appointment.service.impl;

import com.example.hms.appointment.domain.Appointment;
import com.example.hms.appointment.domain.AppointmentStatus;
import com.example.hms.appointment.dto.AppointmentRequest;
import com.example.hms.appointment.dto.AppointmentResponse;
import com.example.hms.appointment.repository.AppointmentRepository;
import com.example.hms.appointment.service.AppointmentService;
import com.example.hms.common.exception.BusinessValidationException;
import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.doctor.domain.Doctor;
import com.example.hms.doctor.repository.DoctorRepository;
import com.example.hms.notification.service.NotificationService;
import com.example.hms.patient.domain.Patient;
import com.example.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  PatientRepository patientRepository,
                                  DoctorRepository doctorRepository,
                                  NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
    }

    @Override
    public AppointmentResponse book(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.doctorId()));

        boolean slotTaken = appointmentRepository.existsByDoctorIdAndAppointmentTimeAndStatus(
                doctor.getId(),
                request.appointmentTime(),
                AppointmentStatus.SCHEDULED
        );
        if (slotTaken) {
            throw new BusinessValidationException("Doctor already has an appointment at this time");
        }

        Appointment appointment = new Appointment(patient, doctor, request.appointmentTime(), request.reason());
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.sendAppointmentBooked(
                patient.getFullName(),
                patient.getPhone(),
                doctor.getFullName(),
                saved.getAppointmentTime(),
                saved.getReason()
        );
        return toResponse(saved);
    }

    @Override
    public AppointmentResponse complete(Long appointmentId) {
        Appointment appointment = getAppointment(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessValidationException("Only scheduled appointments can be completed");
        }
        appointment.markCompleted();
        return toResponse(appointment);
    }

    @Override
    public AppointmentResponse cancel(Long appointmentId) {
        Appointment appointment = getAppointment(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessValidationException("Completed appointment cannot be cancelled");
        }
        appointment.cancel();
        notificationService.sendAppointmentCancelled(
                appointment.getPatient().getFullName(),
                appointment.getPatient().getPhone(),
                appointment.getDoctor().getFullName(),
                appointment.getAppointmentTime()
        );
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    private Appointment getAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getReason()
        );
    }
}
