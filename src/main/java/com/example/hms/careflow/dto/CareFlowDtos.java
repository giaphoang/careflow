package com.example.hms.careflow.dto;

import com.example.hms.careflow.domain.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
import org.springframework.data.domain.Page;

public final class CareFlowDtos {
    private CareFlowDtos() {}
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
        public static <T> PageView<T> of(Page<T> value) {
            return new PageView<>(value.getContent(), value.getNumber(), value.getSize(), value.getTotalElements(),
                value.getTotalPages(), value.isLast());
        }
    }
    public record CreateItem(@NotBlank @Size(max=160) String name, @NotBlank @Size(max=80) String udi,
        @NotBlank @Size(max=80) String lot, @NotBlank @Size(max=100) String location,
        @NotBlank @Size(max=80) String category, @NotNull @Future LocalDate expiresOn,
        @Min(0) int totalStock, @Min(0) int reorderPoint) {}
    public record AdjustItem(int stockDelta, boolean quarantined, @NotBlank @Size(max=500) String reason) {}
    public record CreateRequest(@NotNull @Positive Long itemId, @Min(1) int quantity,
        @NotBlank @Size(max=100) String department, @NotBlank @Size(max=500) String purpose) {}
    public record Review(@NotNull Boolean approve, @NotBlank @Size(max=500) String reason) {}
    public record Reason(@NotBlank @Size(max=500) String reason) {}
    public record ItemView(Long id, String name, String udi, String lot, String location, String category,
                           LocalDate expiresOn, int totalStock, int reservedStock, int availableStock,
                           int reorderPoint, boolean quarantined, long version) {
        public static ItemView of(InventoryItem i) {
            return new ItemView(i.getId(), i.getName(), i.getUdi(), i.getLot(), i.getLocation(), i.getCategory(),
                i.getExpiresOn(), i.getTotalStock(), i.getReservedStock(), i.getAvailableStock(),
                i.getReorderPoint(), i.isQuarantined(), i.getVersion());
        }
    }
    public record RequestView(Long id, Long itemId, String itemName, String udi, String lot, int quantity,
        String requestedBy, String department, String purpose, RequestState state, Instant createdAt,
        Instant updatedAt, Instant reservedUntil) {
        public static RequestView of(SupplyRequest r) {
            return new RequestView(r.getId(), r.getItem().getId(), r.getItem().getName(), r.getItem().getUdi(),
                r.getItem().getLot(), r.getQuantity(), r.getRequestedBy(), r.getDepartment(), r.getPurpose(),
                r.getState(), r.getCreatedAt(), r.getUpdatedAt(), r.getReservedUntil());
        }
    }
    public record Receipt(Long id, Long requestId, Long itemId, int quantity, String fulfilledBy,
                          Instant fulfilledAt, String idempotencyKey) {
        public static Receipt of(Fulfillment f) {
            return new Receipt(f.getId(), f.getRequest().getId(), f.getItemId(), f.getQuantity(),
                f.getFulfilledBy(), f.getFulfilledAt(), f.getIdempotencyKey());
        }
    }
}
