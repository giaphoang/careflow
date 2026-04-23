package com.example.hms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class HospitalManagementSystemApplication {

    public static void main(String[] args) {
        // Some local Windows/JVM setups surface the legacy "Asia/Calcutta" zone,
        // which recent PostgreSQL versions reject on connection startup.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
        SpringApplication.run(HospitalManagementSystemApplication.class, args);
    }
}
