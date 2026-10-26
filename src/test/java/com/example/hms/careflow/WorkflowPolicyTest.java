package com.example.hms.careflow;

import com.example.hms.careflow.domain.RequestState;
import com.example.hms.careflow.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class WorkflowPolicyTest {
    static final Set<String> ALLOWED = Set.of("REQUESTED:APPROVED","REQUESTED:REJECTED","REQUESTED:CANCELLED",
        "APPROVED:RESERVED","APPROVED:CANCELLED","RESERVED:FULFILLED","RESERVED:CANCELLED","RESERVED:EXPIRED");
    static Stream<Arguments> transitions() { return Arrays.stream(RequestState.values()).flatMap(from->Arrays.stream(RequestState.values()).map(to->Arguments.of(from,to))); }
    @ParameterizedTest(name="{0} → {1}") @MethodSource("transitions")
    void allStatePairsHaveExplicitRules(RequestState from, RequestState to) {
        if(ALLOWED.contains(from+":"+to)) assertThatCode(()->WorkflowPolicy.transition(from,to)).doesNotThrowAnyException();
        else assertThatThrownBy(()->WorkflowPolicy.transition(from,to)).isInstanceOf(WorkflowException.class);
    }
    @ParameterizedTest @ValueSource(strings={"REQUESTER","INVENTORY_MANAGER","DOCTOR","RECEPTIONIST"})
    void unauthorizedRolesCannotApprove(String role) { assertThatThrownBy(()->WorkflowPolicy.review(new Actor("reviewer",Set.of(role)),"owner")).isInstanceOf(AccessDeniedException.class); }
    @ParameterizedTest @ValueSource(strings={"ADMIN","CLINICAL_STAFF"})
    void evenPrivilegedUsersCannotSelfApprove(String role) { assertThatThrownBy(()->WorkflowPolicy.review(new Actor("owner",Set.of(role)),"owner")).isInstanceOf(AccessDeniedException.class); }
    @Test void clinicalStaffMayReviewAnotherUser() { assertThatCode(()->WorkflowPolicy.review(new Actor("reviewer",Set.of("CLINICAL_STAFF")),"owner")).doesNotThrowAnyException(); }
    @Test void ownerMayReadOwnRequest() { assertThatCode(()->WorkflowPolicy.read(new Actor("owner",Set.of("REQUESTER")),"owner")).doesNotThrowAnyException(); }
    @Test void requesterCannotReadAnotherUsersRequest() { assertThatThrownBy(()->WorkflowPolicy.read(new Actor("other",Set.of("REQUESTER")),"owner")).isInstanceOf(AccessDeniedException.class); }
    @Test void clinicianCannotCancelAnotherUsersRequest() { assertThatThrownBy(()->WorkflowPolicy.cancel(new Actor("reviewer",Set.of("CLINICAL_STAFF")),"owner")).isInstanceOf(AccessDeniedException.class); }
    @Test void inventoryManagerCanCancelToReleaseStock() { assertThatCode(()->WorkflowPolicy.cancel(new Actor("manager",Set.of("INVENTORY_MANAGER")),"owner")).doesNotThrowAnyException(); }
    @Test void unauthenticatedActorIsRejected() { assertThatThrownBy(()->Actor.from(null)).isInstanceOf(AccessDeniedException.class); }
}
