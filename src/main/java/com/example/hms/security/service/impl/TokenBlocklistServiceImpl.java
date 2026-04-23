package com.example.hms.security.service.impl;

import com.example.hms.security.audit.SecurityAuditService;
import com.example.hms.security.jwt.JwtService;
import com.example.hms.security.model.RevokedToken;
import com.example.hms.security.repository.RevokedTokenRepository;
import com.example.hms.security.service.TokenBlocklistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
@Transactional
public class TokenBlocklistServiceImpl implements TokenBlocklistService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;
    private final SecurityAuditService securityAuditService;

    public TokenBlocklistServiceImpl(RevokedTokenRepository revokedTokenRepository,
                                     JwtService jwtService,
                                     SecurityAuditService securityAuditService) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
        this.securityAuditService = securityAuditService;
    }

    @Override
    public void revoke(String token) {
        if (!jwtService.isTokenValid(token)) {
            return;
        }

        String tokenId = jwtService.extractTokenId(token);
        String tokenHash = hashTokenId(tokenId);
        if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        Instant expiry = jwtService.extractExpiration(token).toInstant();
        revokedTokenRepository.save(new RevokedToken(tokenHash, expiry));
        String tokenType = jwtService.extractTokenType(token);
        String username = safeExtractUsername(token);
        securityAuditService.tokenRevoked(tokenType, username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return revokedTokenRepository.existsByTokenHash(hashTokenId(tokenId));
    }

    private String hashTokenId(String tokenId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenId.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String safeExtractUsername(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
