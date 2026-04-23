package com.example.hms.appointment.domain;

import com.example.hms.doctor.domain.Doctor;
import com.example.hms.patient.domain.Patient;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(nullable = false)
    private String reason;

    protected Appointment() {}

    public Appointment(Patient patient, Doctor doctor, LocalDateTime appointmentTime, String reason) {
        this.patient = patient;
        this.doctor = doctor;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
        this.status = AppointmentStatus.SCHEDULED;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public String getReason() { return reason; }

    public void markCompleted() { this.status = AppointmentStatus.COMPLETED; }
    public void cancel() { this.status = AppointmentStatus.CANCELLED; }
}
