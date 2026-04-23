package com.example.hms.security.service.impl;

import com.example.hms.common.exception.BusinessValidationException;
import com.example.hms.doctor.domain.Doctor;
import com.example.hms.doctor.repository.DoctorRepository;
import com.example.hms.security.dto.AuthResponse;
import com.example.hms.security.dto.CreateUserRequest;
import com.example.hms.security.dto.LoginRequest;
import com.example.hms.security.dto.LogoutRequest;
import com.example.hms.security.dto.RefreshTokenRequest;
import com.example.hms.security.dto.UpdateUserRoleRequest;
import com.example.hms.security.dto.UserSummaryResponse;
import com.example.hms.security.audit.SecurityAuditService;
import com.example.hms.security.jwt.JwtService;
import com.example.hms.security.model.AppUser;
import com.example.hms.security.model.Role;
import com.example.hms.security.repository.AppUserRepository;
import com.example.hms.security.service.AuthService;
import com.example.hms.security.service.TokenBlocklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlocklistService tokenBlocklistService;
    private final SecurityAuditService securityAuditService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           AppUserRepository appUserRepository,
                           DoctorRepository doctorRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           TokenBlocklistService tokenBlocklistService,
                           SecurityAuditService securityAuditService) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlocklistService = tokenBlocklistService;
        this.securityAuditService = securityAuditService;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            securityAuditService.loginFailure(request.username(), "invalid_credentials");
            throw new BusinessValidationException("Invalid username or password");
        }

        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessValidationException("Invalid username or password"));

        try {
            String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getRole().name());
            securityAuditService.loginSuccess(user.getUsername(), user.getRole().name());
            return new AuthResponse(accessToken, refreshToken, "Bearer", user.getUsername(), user.getRole().name());
        } catch (RuntimeException ex) {
            logger.error("Login failed after authentication succeeded for user={}", user.getUsername(), ex);
            throw ex;
        }
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            securityAuditService.refreshFailure(null, "invalid_refresh_token");
            throw new BusinessValidationException("Invalid refresh token");
        }
        String refreshTokenId = jwtService.extractTokenId(refreshToken);
        if (tokenBlocklistService.isRevoked(refreshTokenId)) {
            String revokedUser = safeExtractUsername(refreshToken);
            securityAuditService.refreshFailure(revokedUser, "refresh_token_revoked");
            throw new BusinessValidationException("Refresh token is revoked");
        }

        String username = jwtService.extractUsername(refreshToken);
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessValidationException("User not found for refresh token"));

        tokenBlocklistService.revoke(refreshToken);
        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getRole().name());
        securityAuditService.refreshSuccess(user.getUsername());
        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", user.getUsername(), user.getRole().name());
    }

    @Override
    public void logout(String authorizationHeader, LogoutRequest request) {
        String username = null;
        boolean accessRevoked = false;
        boolean refreshRevoked = false;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            username = safeExtractUsername(accessToken);
            tokenBlocklistService.revoke(accessToken);
            accessRevoked = true;
        }

        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            if (username == null) {
                username = safeExtractUsername(request.refreshToken());
            }
            tokenBlocklistService.revoke(request.refreshToken());
            refreshRevoked = true;
        }

        securityAuditService.logout(username, accessRevoked, refreshRevoked);
    }

    @Override
    public void createUser(CreateUserRequest request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new BusinessValidationException("Username already exists");
        }

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        appUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUsers() {
        return appUserRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new UserSummaryResponse(user.getId(), user.getUsername(), user.getRole()))
                .toList();
    }

    @Override
    public void updateUserRole(Long userId, UpdateUserRoleRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        if (request.role() == Role.DOCTOR) {
            ensureDoctorProfile(user, request);
        }

        user.updateRole(request.role());
        appUserRepository.save(user);
    }

    private void ensureDoctorProfile(AppUser user, UpdateUserRoleRequest request) {
        String email = normalizeDoctorEmail(user, request);
        doctorRepository.findByEmail(email).orElseGet(() -> doctorRepository.save(
                new Doctor(
                        requireDoctorField(request.fullName(), "Doctor full name is required when promoting to doctor"),
                        requireDoctorField(request.specialization(), "Doctor specialization is required when promoting to doctor"),
                        email
                )
        ));
    }

    private String normalizeDoctorEmail(AppUser user, UpdateUserRoleRequest request) {
        String candidate = request.email();
        if (candidate == null || candidate.isBlank()) {
            candidate = user.getUsername();
        }
        candidate = candidate.trim();
        if (!candidate.contains("@")) {
            throw new BusinessValidationException("Doctor email is required when promoting to doctor");
        }
        return candidate;
    }

    private String requireDoctorField(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessValidationException(message);
        }
        return value.trim();
    }

    private String safeExtractUsername(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
