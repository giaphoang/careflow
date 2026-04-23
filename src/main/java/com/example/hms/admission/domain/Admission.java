package com.example.hms.admission.domain;

import com.example.hms.patient.domain.Patient;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "admissions")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(nullable = false)
    private String ward;

    @Column(nullable = false)
    private LocalDate admittedOn;

    private LocalDate dischargedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdmissionStatus status;

    protected Admission() {}

    public Admission(Patient patient, String ward, LocalDate admittedOn) {
        this.patient = patient;
        this.ward = ward;
        this.admittedOn = admittedOn;
        this.status = AdmissionStatus.ADMITTED;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public String getWard() { return ward; }
    public LocalDate getAdmittedOn() { return admittedOn; }
    public LocalDate getDischargedOn() { return dischargedOn; }
    public AdmissionStatus getStatus() { return status; }

    public void discharge(LocalDate dischargeDate) {
        this.dischargedOn = dischargeDate;
        this.status = AdmissionStatus.DISCHARGED;
    }
}
