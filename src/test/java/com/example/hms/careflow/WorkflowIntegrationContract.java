package com.example.hms.careflow;

import com.example.hms.careflow.domain.*;
import com.example.hms.careflow.dto.CareFlowDtos.*;
import com.example.hms.careflow.repository.*;
import com.example.hms.careflow.service.*;
import com.example.hms.notification.repository.NotificationOutboxRepository;
import com.example.hms.notification.service.NotificationOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"careflow.demo-enabled=false","careflow.expiry-enabled=false","notification.outbox.poll-ms=36000000"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class WorkflowIntegrationContract {
    @Autowired CareFlowService service;
    @Autowired InventoryRepository inventory;
    @Autowired RequestRepository requests;
    @Autowired AuditRepository audits;
    @Autowired FulfillmentRepository fulfillments;
    @Autowired NotificationOutboxRepository notifications;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean Clock clock;
    @SpyBean NotificationOutboxService outbox;
    final Actor owner=new Actor("owner",Set.of("REQUESTER"));
    final Actor manager=new Actor("manager",Set.of("INVENTORY_MANAGER"));
    final Actor clinical=new Actor("clinical",Set.of("CLINICAL_STAFF"));
    final Actor admin=new Actor("admin",Set.of("ADMIN"));
    final Instant now=Instant.parse("2030-01-01T10:00:00Z");

    @BeforeEach void resetDatabase() {
        reset(outbox);when(clock.instant()).thenReturn(now);when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        fulfillments.deleteAllInBatch();audits.deleteAllInBatch();requests.deleteAllInBatch();inventory.deleteAllInBatch();notifications.deleteAllInBatch();
    }
    ItemView item(int stock) { return service.createItem(manager,new CreateItem("Synthetic cardiac kit","SYN-UDI-"+UUID.randomUUID(),"SIM-LOT","Store A","Cardiovascular",LocalDate.of(2035,1,1),stock,2)); }
    RequestView request(Long itemId, int quantity) { return service.submit(owner,new CreateRequest(itemId,quantity,"Cardiac services","Synthetic replenishment")); }
    RequestView approved(Long itemId, int quantity) { var r=request(itemId,quantity);return service.review(clinical,r.id(),new Review(true,"Need confirmed")); }
    RequestView reserved(Long itemId, int quantity) { var r=approved(itemId,quantity);return service.reserve(manager,r.id()); }
    InventoryItem persisted(Long id) { return inventory.findById(id).orElseThrow(); }

    @Test void CF01_atomicReservationPreservesTotalStock() { var i=item(5);var r=approved(i.id(),3);service.reserve(manager,r.id());assertThat(persisted(i.id()).getReservedStock()).isEqualTo(3);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(5); }
    @Test void CF01_insufficientStockRollsBackStateAndAudit() { var i=item(1);var r=approved(i.id(),2);long before=audits.count();assertThatThrownBy(()->service.reserve(manager,r.id())).isInstanceOf(WorkflowException.class);assertThat(persisted(i.id()).getReservedStock()).isZero();assertThat(service.request(owner,r.id()).state()).isEqualTo(RequestState.APPROVED);assertThat(audits.count()).isEqualTo(before); }
    @Test void CF02_sameKeyReturnsExactlyTheOriginalReceipt() { var i=item(5);var r=reserved(i.id(),2);var first=service.fulfill(manager,r.id(),"retry-key");var retry=service.fulfill(manager,r.id(),"retry-key");assertThat(retry).isEqualTo(first);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(3);assertThat(fulfillments.count()).isEqualTo(1); }
    @Test void CF02_differentKeyCannotFulfillSameRequestTwice() { var i=item(5);var r=reserved(i.id(),2);service.fulfill(manager,r.id(),"first-key");assertThatThrownBy(()->service.fulfill(manager,r.id(),"new-key")).isInstanceOf(WorkflowException.class);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(3); }
    @Test void CF02_keyCannotBeReusedAcrossRequests() { var i=item(10);var a=reserved(i.id(),2);var b=reserved(i.id(),2);service.fulfill(manager,a.id(),"shared-key");assertThatThrownBy(()->service.fulfill(manager,b.id(),"shared-key")).isInstanceOf(WorkflowException.class);assertThat(service.request(owner,b.id()).state()).isEqualTo(RequestState.RESERVED);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(8); }
    @Test void CF03_selfApprovalIsForbiddenEvenForAdmin() { var i=item(5);var r=service.submit(admin,new CreateRequest(i.id(),1,"Central","Synthetic"));assertThatThrownBy(()->service.review(admin,r.id(),new Review(true,"Self"))).isInstanceOf(AccessDeniedException.class);assertThat(service.request(admin,r.id()).state()).isEqualTo(RequestState.REQUESTED); }
    @Test void CF03_requesterCannotApprove() { var r=request(item(5).id(),1);assertThatThrownBy(()->service.review(owner,r.id(),new Review(true,"Self"))).isInstanceOf(AccessDeniedException.class); }
    @Test void CF03_requesterCannotReadAnotherOwnersRequest() { var r=request(item(5).id(),1);assertThatThrownBy(()->service.request(new Actor("other",Set.of("REQUESTER")),r.id())).isInstanceOf(AccessDeniedException.class);assertThat(service.requests(new Actor("other",Set.of("REQUESTER")),PageRequest.of(0,20))).isEmpty(); }
    @Test void CF04_outboxFailureRollsBackStockStateAndAudit() { var i=item(5);var r=reserved(i.id(),2);long before=audits.count();doThrow(new IllegalStateException("Simulated outbox failure")).when(outbox).enqueue(any());assertThatThrownBy(()->service.fulfill(manager,r.id(),"rollback-key")).isInstanceOf(IllegalStateException.class);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(5);assertThat(persisted(i.id()).getReservedStock()).isEqualTo(2);assertThat(service.request(owner,r.id()).state()).isEqualTo(RequestState.RESERVED);assertThat(fulfillments.count()).isZero();assertThat(audits.count()).isEqualTo(before); }
    @Test void CF05_everyTransitionHasPersistentActorAndStates() { var i=item(5);var r=reserved(i.id(),1);service.fulfill(manager,r.id(),"audit-key");var history=service.audit(owner,r.id(),PageRequest.of(0,20)).getContent();assertThat(history).extracting(AuditEntry::getNewState).containsExactly("REQUESTED","APPROVED","RESERVED","FULFILLED");assertThat(history).extracting(AuditEntry::getActor).containsExactly("owner","clinical","manager","manager");assertThat(history.get(3).getPreviousState()).isEqualTo("RESERVED");assertThat(history).allSatisfy(a->assertThat(a.getOccurredAt()).isEqualTo(now)); }
    @Test void cancelReleasesExactlyOnce() { var i=item(5);var r=reserved(i.id(),3);service.cancel(owner,r.id(),"Not needed");assertThat(persisted(i.id()).getTotalStock()).isEqualTo(5);assertThat(persisted(i.id()).getReservedStock()).isZero();assertThatThrownBy(()->service.cancel(owner,r.id(),"Again")).isInstanceOf(WorkflowException.class); }
    @Test void expiryReleasesStockAndRecordsSystemActor() { var i=item(5);var r=reserved(i.id(),3);when(clock.instant()).thenReturn(now.plusSeconds(1800));service.expire(new Actor("system:reservation-expiry",Set.of("INVENTORY_MANAGER")),r.id());assertThat(persisted(i.id()).getReservedStock()).isZero();assertThat(service.request(owner,r.id()).state()).isEqualTo(RequestState.EXPIRED); }
    @Test void expiredReservationCannotBeFulfilled() { var i=item(5);var r=reserved(i.id(),2);when(clock.instant()).thenReturn(now.plusSeconds(1800));assertThatThrownBy(()->service.fulfill(manager,r.id(),"expired-key")).isInstanceOf(WorkflowException.class);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(5); }
    @Test void qualityHoldAfterReservationBlocksFulfillmentButAllowsRelease() { var i=item(5);var r=reserved(i.id(),2);service.adjustItem(manager,i.id(),new AdjustItem(0,true,"Synthetic recall hold"));assertThatThrownBy(()->service.fulfill(manager,r.id(),"hold-key")).isInstanceOf(WorkflowException.class);service.cancel(manager,r.id(),"Quality hold");assertThat(persisted(i.id()).getReservedStock()).isZero();assertThat(persisted(i.id()).getTotalStock()).isEqualTo(5); }
    @Test void lowStockAndFulfillmentUseFoundationOutbox() { var i=item(3);var r=reserved(i.id(),1);service.fulfill(manager,r.id(),"outbox-key");assertThat(notifications.findAll()).extracting(n->n.getEventType()).contains("LOW_STOCK","SUPPLY_FULFILLED"); }
    @Test void databaseConstraintRejectsNegativeStock() { var i=item(5);var tx=new TransactionTemplate(transactionManager);assertThatThrownBy(()->tx.executeWithoutResult(s->{var em=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);em.createNativeQuery("UPDATE cf_inventory SET total_stock=-1 WHERE id=:id").setParameter("id",i.id()).executeUpdate();})).isInstanceOf(RuntimeException.class);assertThat(persisted(i.id()).getTotalStock()).isEqualTo(5); }
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Test void apiRejectsUnauthenticatedRequests() throws Exception { mvc.perform(get("/api/careflow/inventory")).andExpect(status().isUnauthorized()); }
    @Test void apiRejectsUnauthorizedInventoryWrite() throws Exception { mvc.perform(post("/api/careflow/inventory").with(user("owner").roles("REQUESTER")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new CreateItem("Synthetic","UDI","LOT","A","Surgical",LocalDate.of(2035,1,1),5,1)))).andExpect(status().isForbidden()); }
    @Test void apiRejectsInvalidQuantity() throws Exception { mvc.perform(post("/api/careflow/requests").with(user("owner").roles("REQUESTER")).contentType(MediaType.APPLICATION_JSON).content("{\"itemId\":1,\"quantity\":0,\"department\":\"Cardiac\",\"purpose\":\"Synthetic\"}")).andExpect(status().isBadRequest()); }
    @Test void apiRequiresIdempotencyHeader() throws Exception { mvc.perform(post("/api/careflow/requests/1/fulfill").with(user("manager").roles("INVENTORY_MANAGER"))).andExpect(status().isBadRequest()); }
    @Test void apiRestrictsGlobalAuditToAdministrator() throws Exception { mvc.perform(get("/api/careflow/audit").with(user("owner").roles("REQUESTER"))).andExpect(status().isForbidden());mvc.perform(get("/api/careflow/audit").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()); }
    @Test void apiBoundsPagination() throws Exception { mvc.perform(get("/api/careflow/inventory?size=201").with(user("owner").roles("REQUESTER"))).andExpect(status().isBadRequest()); }
    @Test void apiReturnsConflictForOutOfOrderTransition() throws Exception { var r=request(item(5).id(),1);mvc.perform(post("/api/careflow/requests/{id}/reserve",r.id()).with(user("manager").roles("INVENTORY_MANAGER"))).andExpect(status().isConflict()); }
    @Test void apiHidesOtherOwnersAuditHistory() throws Exception { var r=request(item(5).id(),1);mvc.perform(get("/api/careflow/audit?requestId="+r.id()).with(user("other").roles("REQUESTER"))).andExpect(status().isForbidden()); }
}
