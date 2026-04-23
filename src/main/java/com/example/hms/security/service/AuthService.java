package com.example.hms.security.service;

import com.example.hms.security.dto.AuthResponse;
import com.example.hms.security.dto.CreateUserRequest;
import com.example.hms.security.dto.LoginRequest;
import com.example.hms.security.dto.LogoutRequest;
import com.example.hms.security.dto.RefreshTokenRequest;
import com.example.hms.security.dto.UpdateUserRoleRequest;
import com.example.hms.security.dto.UserSummaryResponse;

import java.util.List;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String authorizationHeader, LogoutRequest request);

    void createUser(CreateUserRequest request);
    List<UserSummaryResponse> getUsers();
    void updateUserRole(Long userId, UpdateUserRoleRequest request);
}
