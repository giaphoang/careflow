package com.example.hms.careflow.repository;

import com.example.hms.careflow.domain.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {
    Page<AuditEntry> findByResourceTypeAndResourceId(String type, Long id, Pageable page);
}
