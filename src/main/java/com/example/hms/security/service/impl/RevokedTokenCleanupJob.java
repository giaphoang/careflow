package com.example.hms.security.service.impl;

import com.example.hms.security.repository.RevokedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class RevokedTokenCleanupJob {

    private final RevokedTokenRepository revokedTokenRepository;

    public RevokedTokenCleanupJob(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Scheduled(fixedDelayString = "${security.revoked-token-cleanup-ms:3600000}")
    @Transactional
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
