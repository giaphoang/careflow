package com.example.hms.careflow.repository;

import com.example.hms.careflow.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface RequestRepository extends JpaRepository<SupplyRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SupplyRequest r where r.id = :id")
    Optional<SupplyRequest> lockById(Long id);
    Page<SupplyRequest> findByRequestedBy(String requestedBy, Pageable pageable);
    @Query("select r.id from SupplyRequest r where r.state = 'RESERVED' and r.reservedUntil <= :now order by r.id")
    List<Long> findExpired(Instant now, Pageable pageable);
}
