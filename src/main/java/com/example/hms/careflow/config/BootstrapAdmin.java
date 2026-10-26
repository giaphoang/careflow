package com.example.hms.careflow.config;

import com.example.hms.security.dto.CreateUserRequest;
import com.example.hms.security.model.Role;
import com.example.hms.security.repository.AppUserRepository;
import com.example.hms.security.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="careflow.bootstrap-admin.username")
public class BootstrapAdmin implements CommandLineRunner {
    private final AuthService auth;
    private final AppUserRepository users;
    private final String username;
    private final String password;
    public BootstrapAdmin(AuthService auth, AppUserRepository users,
                          @Value("${careflow.bootstrap-admin.username}") String username,
                          @Value("${careflow.bootstrap-admin.password}") String password) {
        this.auth=auth;this.users=users;this.username=username;this.password=password;
    }
    @Override public void run(String... args) {
        if (!users.existsByUsername(username)) {
            if (password.length()<12) throw new IllegalArgumentException("Bootstrap administrator password must be at least 12 characters");
            auth.createUser(new CreateUserRequest(username,password, Role.ADMIN));
        }
    }
}
