package com.example.hms.security.controller;

import com.example.hms.common.api.ApiResponse;
import com.example.hms.security.dto.AuthResponse;
import com.example.hms.security.dto.CreateUserRequest;
import com.example.hms.security.dto.LoginRequest;
import com.example.hms.security.dto.LogoutRequest;
import com.example.hms.security.dto.RefreshTokenRequest;
import com.example.hms.security.dto.UpdateUserRoleRequest;
import com.example.hms.security.dto.UserSummaryResponse;
import com.example.hms.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                    @RequestBody(required = false) LogoutRequest request) {
        authService.logout(authorizationHeader, request);
        return ApiResponse.success("Logout successful", null);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
        authService.createUser(request);
        return ApiResponse.success("User created", null);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserSummaryResponse>> getUsers() {
        return ApiResponse.success("Users fetched", authService.getUsers());
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateUserRole(@PathVariable Long userId,
                                            @Valid @RequestBody UpdateUserRoleRequest request) {
        authService.updateUserRole(userId, request);
        return ApiResponse.success("User role updated", null);
    }
}
