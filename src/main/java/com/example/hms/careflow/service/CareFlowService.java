package com.example.hms.careflow.service;

import com.example.hms.careflow.domain.*;
import com.example.hms.careflow.dto.CareFlowDtos.*;
import com.example.hms.careflow.repository.*;
import com.example.hms.common.exception.ResourceNotFoundException;
import com.example.hms.notification.model.*;
import com.example.hms.notification.service.NotificationOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Objects;
import static com.example.hms.careflow.domain.RequestState.*;

@Service
@Transactional
public class CareFlowService {
    private final InventoryRepository inventory;
    private final RequestRepository requests;
    private final AuditRepository audits;
    private final FulfillmentRepository fulfillments;
    private final NotificationOutboxService outbox;
    private final Clock clock;
    private final int reservationMinutes;

    public CareFlowService(InventoryRepository inventory, RequestRepository requests, AuditRepository audits,
                          FulfillmentRepository fulfillments, NotificationOutboxService outbox, Clock clock,
                          @Value("${careflow.reservation-minutes:30}") int reservationMinutes) {
        this.inventory = inventory; this.requests = requests; this.audits = audits;
        this.fulfillments = fulfillments; this.outbox = outbox; this.clock = clock;
        this.reservationMinutes = reservationMinutes;
    }
    @Transactional(readOnly = true)
    public Page<ItemView> inventory(Actor actor, Pageable page) {
        actor.requireCareFlow(); return inventory.findAll(page).map(ItemView::of);
    }
    public ItemView createItem(Actor actor, CreateItem input) {
        actor.require("INVENTORY_MANAGER");
        if (!input.expiresOn().isAfter(LocalDate.now(clock))) throw new WorkflowException("Expiry must be in the future");
        InventoryItem item = inventory.saveAndFlush(new InventoryItem(input.name(), input.udi(), input.lot(),
            input.location(), input.category(), input.expiresOn(), input.totalStock(), input.reorderPoint()));
        audit(actor, "ITEM_CREATED", "INVENTORY", item.getId(), null, stockState(item), "Synthetic inventory lot created");
        return ItemView.of(item);
    }
    public ItemView adjustItem(Actor actor, Long id, AdjustItem input) {
        actor.require("INVENTORY_MANAGER");
        InventoryItem item = lockItem(id);
        String previous = stockState(item);
        item.adjust(input.stockDelta(), input.quarantined());
        audit(actor, "ITEM_ADJUSTED", "INVENTORY", id, previous, stockState(item), input.reason());
        return ItemView.of(item);
    }
    public RequestView submit(Actor actor, CreateRequest input) {
        actor.require("REQUESTER", "CLINICAL_STAFF");
        InventoryItem item = inventory.findById(input.itemId()).orElseThrow(() -> missing("Inventory item", input.itemId()));
        item.ensureUsable(LocalDate.now(clock));
        SupplyRequest request = requests.saveAndFlush(new SupplyRequest(item, input.quantity(), actor.name(),
            input.department(), input.purpose(), clock.instant()));
        audit(actor, "REQUESTED", "REQUEST", request.getId(), null, REQUESTED.name(), input.purpose());
        return RequestView.of(request);
    }
    @Transactional(readOnly = true)
    public Page<RequestView> requests(Actor actor, Pageable page) {
        actor.requireCareFlow();
        boolean ownOnly = actor.has("REQUESTER") && !actor.has("ADMIN") && !actor.has("CLINICAL_STAFF") && !actor.has("INVENTORY_MANAGER");
        return (ownOnly ? requests.findByRequestedBy(actor.name(), page) : requests.findAll(page)).map(RequestView::of);
    }
    @Transactional(readOnly = true)
    public RequestView request(Actor actor, Long id) {
        SupplyRequest r = requests.findById(id).orElseThrow(() -> missing("Request", id));
        WorkflowPolicy.read(actor, r.getRequestedBy()); return RequestView.of(r);
    }
    public RequestView review(Actor actor, Long id, Review input) {
        SupplyRequest r = lockRequest(id);
        WorkflowPolicy.review(actor, r.getRequestedBy());
        move(actor, r, input.approve() ? APPROVED : REJECTED, input.reason());
        return RequestView.of(r);
    }
    public RequestView reserve(Actor actor, Long id) {
        actor.require("INVENTORY_MANAGER");
        SupplyRequest r = lockRequest(id);
        WorkflowPolicy.transition(r.getState(), RESERVED);
        InventoryItem item = lockItem(r.getItem().getId());
        item.reserve(r.getQuantity(), LocalDate.now(clock));
        r.setReservedUntil(clock.instant().plus(Duration.ofMinutes(reservationMinutes)));
        move(actor, r, RESERVED, "Stock reserved for " + reservationMinutes + " minutes");
        if (item.getAvailableStock() <= item.getReorderPoint()) notify("LOW_STOCK", "inventory-simulation", "Low available stock for lot " + item.getLot());
        return RequestView.of(r);
    }
    public Receipt fulfill(Actor actor, Long id, String key) {
        actor.require("INVENTORY_MANAGER");
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}")) throw new WorkflowException("A 1–100 character Idempotency-Key is required (letters, digits, . _ : -)");
        // All operations acquire the request row before the inventory row. A retry waits for the first commit.
        SupplyRequest r = lockRequest(id);
        var existing = fulfillments.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            if (!Objects.equals(existing.get().getRequest().getId(), id)) throw new WorkflowException("Idempotency key already belongs to a different request");
            return Receipt.of(existing.get());
        }
        WorkflowPolicy.transition(r.getState(), FULFILLED);
        if (r.getReservedUntil() == null || !r.getReservedUntil().isAfter(clock.instant()))
            throw new WorkflowException("Reservation has expired; release it before allocating again");
        lockItem(r.getItem().getId()).fulfill(r.getQuantity(), LocalDate.now(clock));
        move(actor, r, FULFILLED, "Distribution confirmed; lot " + r.getItem().getLot());
        Fulfillment receipt = fulfillments.saveAndFlush(new Fulfillment(key, r, actor.name(), clock.instant()));
        notify("SUPPLY_FULFILLED", r.getRequestedBy(), "Request CF-" + id + " fulfilled; synthetic lot " + r.getItem().getLot());
        return Receipt.of(receipt);
    }
    public RequestView cancel(Actor actor, Long id, String reason) {
        SupplyRequest r = lockRequest(id);
        WorkflowPolicy.cancel(actor, r.getRequestedBy());
        WorkflowPolicy.transition(r.getState(), CANCELLED);
        releaseIfReserved(r);
        move(actor, r, CANCELLED, reason); return RequestView.of(r);
    }
    public RequestView expire(Actor actor, Long id) {
        actor.require("INVENTORY_MANAGER");
        SupplyRequest r = lockRequest(id);
        WorkflowPolicy.transition(r.getState(), EXPIRED);
        if (r.getReservedUntil() == null || r.getReservedUntil().isAfter(clock.instant()))
            throw new WorkflowException("Reservation is still active");
        releaseIfReserved(r);
        move(actor, r, EXPIRED, "Reservation window elapsed"); return RequestView.of(r);
    }
    @Transactional(readOnly = true)
    public Page<AuditEntry> audit(Actor actor, Long requestId, Pageable page) {
        actor.requireCareFlow();
        if (requestId != null) {
            request(actor, requestId);
            return audits.findByResourceTypeAndResourceId("REQUEST", requestId, page);
        }
        actor.require(); // Only administrators may read the full audit trail.
        return audits.findAll(page);
    }
    private void releaseIfReserved(SupplyRequest r) {
        if (r.getState() == RESERVED) lockItem(r.getItem().getId()).release(r.getQuantity());
    }
    private void move(Actor actor, SupplyRequest r, RequestState next, String detail) {
        WorkflowPolicy.transition(r.getState(), next);
        String previous = r.getState().name(); r.transition(next, clock.instant());
        audit(actor, next.name(), "REQUEST", r.getId(), previous, next.name(), detail);
    }
    private void audit(Actor actor, String action, String type, Long id, String previous, String next, String detail) {
        audits.save(new AuditEntry(actor.name(), action, type, id, previous, next, detail, clock.instant()));
    }
    private void notify(String type, String recipient, String body) {
        // Reuses the foundation's transactional outbox. The dispatcher runs in simulation mode by default.
        outbox.enqueue(new NotificationMessage(NotificationMedium.WHATSAPP, recipient, "CareFlow · " + type, body, type));
    }
    private InventoryItem lockItem(Long id) { return inventory.lockById(id).orElseThrow(() -> missing("Inventory item", id)); }
    private SupplyRequest lockRequest(Long id) { return requests.lockById(id).orElseThrow(() -> missing("Request", id)); }
    private ResourceNotFoundException missing(String type, Long id) { return new ResourceNotFoundException(type + " not found: " + id); }
    private String stockState(InventoryItem i) { return "stock=" + i.getTotalStock() + ",reserved=" + i.getReservedStock() + ",quarantined=" + i.isQuarantined(); }
}
