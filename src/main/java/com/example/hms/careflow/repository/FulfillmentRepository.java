package com.example.hms.careflow.repository;

import com.example.hms.careflow.domain.Fulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {
    Optional<Fulfillment> findByIdempotencyKey(String key);
}
