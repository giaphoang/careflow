package com.example.hms.security.service;

public interface TokenBlocklistService {
    void revoke(String token);
    boolean isRevoked(String tokenId);
}
