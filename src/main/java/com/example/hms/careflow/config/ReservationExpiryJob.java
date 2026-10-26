package com.example.hms.careflow.config;

import com.example.hms.careflow.repository.RequestRepository;
import com.example.hms.careflow.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.*;
import java.time.Clock;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "careflow.expiry-enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);
    private final RequestRepository requests;
    private final CareFlowService service;
    private final Clock clock;
    public ReservationExpiryJob(RequestRepository requests, CareFlowService service, Clock clock) {
        this.requests = requests; this.service = service; this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${careflow.expiry-poll-ms:60000}")
    public void expire() {
        for (Long id : requests.findExpired(clock.instant(), PageRequest.of(0, 100))) {
            try { service.expire(new Actor("system:reservation-expiry", Set.of("INVENTORY_MANAGER")), id); }
            catch (WorkflowException ignored) { /* Another instance or user already transitioned this request. */ }
            catch (RuntimeException ex) { log.error("Could not expire request {}", id, ex); }
        }
    }
}
