package com.example.hms.doctor.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false, unique = true)
    private String email;

    protected Doctor() {}

    public Doctor(String fullName, String specialization, String email) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getSpecialization() { return specialization; }
    public String getEmail() { return email; }

    public void update(String fullName, String specialization, String email) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.email = email;
    }
}
