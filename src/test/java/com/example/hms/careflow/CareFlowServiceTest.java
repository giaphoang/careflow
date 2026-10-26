package com.example.hms.careflow;

import com.example.hms.careflow.domain.*;
import com.example.hms.careflow.dto.CareFlowDtos.*;
import com.example.hms.careflow.repository.*;
import com.example.hms.careflow.service.*;
import com.example.hms.notification.service.NotificationOutboxService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CareFlowServiceTest {
    @Mock InventoryRepository inventory;
    @Mock RequestRepository requests;
    @Mock AuditRepository audits;
    @Mock FulfillmentRepository fulfillments;
    @Mock NotificationOutboxService outbox;
    CareFlowService service;
    Actor manager=new Actor("manager",Set.of("INVENTORY_MANAGER"));
    Actor clinical=new Actor("clinical",Set.of("CLINICAL_STAFF"));
    Instant now=Instant.parse("2030-01-01T10:00:00Z");
    InventoryItem item;
    SupplyRequest request;
    @BeforeEach void setup() {
        service=new CareFlowService(inventory,requests,audits,fulfillments,outbox,Clock.fixed(now,ZoneOffset.UTC),30);
        item=new InventoryItem("Synthetic device","SYN-1","LOT-1","Store A","Cardiac",LocalDate.of(2031,1,1),10,2);
        request=new SupplyRequest(item,2,"owner","Cardiac","Synthetic replenishment",now);
    }
    @Test void approvalDoesNotTouchInventory() {
        when(requests.lockById(1L)).thenReturn(Optional.of(request));
        service.review(clinical,1L,new Review(true,"Confirmed"));
        assertThat(request.getState()).isEqualTo(RequestState.APPROVED);verifyNoInteractions(inventory,outbox,fulfillments);verify(audits).save(any());
    }
    @Test void rejectionIsAudited() {
        when(requests.lockById(1L)).thenReturn(Optional.of(request));service.review(clinical,1L,new Review(false,"Not required"));
        ArgumentCaptor<AuditEntry> audit=ArgumentCaptor.forClass(AuditEntry.class);verify(audits).save(audit.capture());
        assertThat(audit.getValue().getPreviousState()).isEqualTo("REQUESTED");assertThat(audit.getValue().getNewState()).isEqualTo("REJECTED");
    }
    @Test void cannotReserveBeforeApproval() {
        when(requests.lockById(1L)).thenReturn(Optional.of(request));assertThatThrownBy(()->service.reserve(manager,1L)).isInstanceOf(WorkflowException.class);verifyNoInteractions(inventory,audits);
    }
    @Test void blankKeyNeverAccessesDatabase() { assertThatThrownBy(()->service.fulfill(manager,1L," ")).isInstanceOf(WorkflowException.class);verifyNoInteractions(requests,inventory,fulfillments); }
    @Test void missingInventoryProducesNotFound() { when(inventory.lockById(9L)).thenReturn(Optional.empty());assertThatThrownBy(()->service.adjustItem(manager,9L,new AdjustItem(1,false,"Restock"))).hasMessageContaining("not found"); }
    @Test void invalidTransitionDoesNotWriteAudit() {
        request.transition(RequestState.FULFILLED,now);when(requests.lockById(1L)).thenReturn(Optional.of(request));
        assertThatThrownBy(()->service.cancel(manager,1L,"Cancel")).isInstanceOf(WorkflowException.class);verifyNoInteractions(audits,inventory);
    }
    @Test void earlyExpiryDoesNotReleaseStock() {
        request.transition(RequestState.RESERVED,now);request.setReservedUntil(now.plusSeconds(60));when(requests.lockById(1L)).thenReturn(Optional.of(request));
        assertThatThrownBy(()->service.expire(manager,1L)).isInstanceOf(WorkflowException.class);verifyNoInteractions(inventory,audits);
    }
}
