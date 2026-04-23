package com.example.hms.security.audit;

public interface SecurityAuditService {
    void loginSuccess(String username, String role);
    void loginFailure(String username, String reason);
    void refreshSuccess(String username);
    void refreshFailure(String username, String reason);
    void logout(String username, boolean accessRevoked, boolean refreshRevoked);
    void tokenRevoked(String tokenType, String username);
}
