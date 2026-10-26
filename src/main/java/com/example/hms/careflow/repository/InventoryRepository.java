package com.example.hms.careflow.repository;

import com.example.hms.careflow.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.id = :id")
    Optional<InventoryItem> lockById(Long id);
}
