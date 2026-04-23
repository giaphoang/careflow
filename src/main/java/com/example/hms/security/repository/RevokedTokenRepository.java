package com.example.hms.security.repository;

import com.example.hms.security.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant instant);
}
