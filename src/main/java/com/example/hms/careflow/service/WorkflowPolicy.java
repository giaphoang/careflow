package com.example.hms.careflow.service;

import com.example.hms.careflow.domain.RequestState;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static com.example.hms.careflow.domain.RequestState.*;

public final class WorkflowPolicy {
    private WorkflowPolicy() {}
    private static final Map<RequestState, Set<RequestState>> TRANSITIONS = Map.of(
        REQUESTED, Set.of(APPROVED, REJECTED, CANCELLED),
        APPROVED, Set.of(RESERVED, CANCELLED),
        RESERVED, Set.of(FULFILLED, CANCELLED, EXPIRED));

    public static void transition(RequestState from, RequestState to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to))
            throw new WorkflowException("Cannot transition from " + from + " to " + to);
    }
    public static void review(Actor actor, String requester) {
        actor.require("CLINICAL_STAFF");
        if (actor.name().equals(requester)) throw new AccessDeniedException("Self approval or rejection is forbidden");
    }
    public static void read(Actor actor, String requester) {
        actor.requireCareFlow();
        if (actor.has("REQUESTER") && !actor.has("ADMIN") && !actor.has("CLINICAL_STAFF")
                && !actor.has("INVENTORY_MANAGER") && !actor.name().equals(requester))
            throw new AccessDeniedException("You can only access your own requests");
    }
    public static void cancel(Actor actor, String requester) {
        actor.requireCareFlow();
        if (!actor.has("ADMIN") && !actor.has("INVENTORY_MANAGER") && !actor.name().equals(requester))
            throw new AccessDeniedException("Only the requester or inventory staff can cancel");
    }
}
