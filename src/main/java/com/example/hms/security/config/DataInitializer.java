package com.example.hms.security.config;

import com.example.hms.security.dto.CreateUserRequest;
import com.example.hms.security.model.Role;
import com.example.hms.security.repository.AppUserRepository;
import com.example.hms.security.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(AuthService authService, AppUserRepository appUserRepository) {
        return args -> {
            if (!appUserRepository.existsByUsername("admin")) {
                authService.createUser(new CreateUserRequest("admin", "admin123", Role.ADMIN));
            }
            if (!appUserRepository.existsByUsername("doctor")) {
                authService.createUser(new CreateUserRequest("doctor", "doctor123", Role.DOCTOR));
            }
            if (!appUserRepository.existsByUsername("reception")) {
                authService.createUser(new CreateUserRequest("reception", "recep123", Role.RECEPTIONIST));
            }
        };
    }
}
