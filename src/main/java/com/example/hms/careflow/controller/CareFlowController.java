package com.example.hms.careflow.controller;

import com.example.hms.careflow.domain.AuditEntry;
import com.example.hms.careflow.dto.CareFlowDtos.*;
import com.example.hms.careflow.service.*;
import com.example.hms.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/careflow")
@Validated
public class CareFlowController {
    private final CareFlowService service;
    public CareFlowController(CareFlowService service) { this.service = service; }
    private Pageable page(int page, int size) { return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")); }
    @GetMapping("/inventory")
    public ApiResponse<PageView<ItemView>> inventory(Authentication auth, @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="50") @Min(1) @Max(200) int size) {
        return ApiResponse.success("Inventory", PageView.of(service.inventory(Actor.from(auth), page(page, size))));
    }
    @PostMapping("/inventory")
    public ApiResponse<ItemView> createItem(Authentication auth, @Valid @RequestBody CreateItem input) {
        return ApiResponse.success("Lot created", service.createItem(Actor.from(auth), input));
    }
    @PatchMapping("/inventory/{id}")
    public ApiResponse<ItemView> adjust(Authentication auth, @PathVariable Long id, @Valid @RequestBody AdjustItem input) {
        return ApiResponse.success("Inventory adjusted", service.adjustItem(Actor.from(auth), id, input));
    }
    @GetMapping("/requests")
    public ApiResponse<PageView<RequestView>> requests(Authentication auth, @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="50") @Min(1) @Max(200) int size) {
        return ApiResponse.success("Requests", PageView.of(service.requests(Actor.from(auth), page(page, size))));
    }
    @PostMapping("/requests")
    public ApiResponse<RequestView> submit(Authentication auth, @Valid @RequestBody CreateRequest input) {
        return ApiResponse.success("Request submitted", service.submit(Actor.from(auth), input));
    }
    @GetMapping("/requests/{id}")
    public ApiResponse<RequestView> request(Authentication auth, @PathVariable Long id) {
        return ApiResponse.success("Request", service.request(Actor.from(auth), id));
    }
    @PostMapping("/requests/{id}/review")
    public ApiResponse<RequestView> review(Authentication auth, @PathVariable Long id, @Valid @RequestBody Review input) {
        return ApiResponse.success("Review recorded", service.review(Actor.from(auth), id, input));
    }
    @PostMapping("/requests/{id}/reserve")
    public ApiResponse<RequestView> reserve(Authentication auth, @PathVariable Long id) {
        return ApiResponse.success("Inventory reserved", service.reserve(Actor.from(auth), id));
    }
    @PostMapping("/requests/{id}/fulfill")
    public ApiResponse<Receipt> fulfill(Authentication auth, @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.success("Fulfillment receipt", service.fulfill(Actor.from(auth), id, key));
    }
    @PostMapping("/requests/{id}/cancel")
    public ApiResponse<RequestView> cancel(Authentication auth, @PathVariable Long id, @Valid @RequestBody Reason input) {
        return ApiResponse.success("Request cancelled", service.cancel(Actor.from(auth), id, input.reason()));
    }
    @PostMapping("/requests/{id}/expire")
    public ApiResponse<RequestView> expire(Authentication auth, @PathVariable Long id) {
        return ApiResponse.success("Reservation expired", service.expire(Actor.from(auth), id));
    }
    @GetMapping("/audit")
    public ApiResponse<PageView<AuditEntry>> audit(Authentication auth, @RequestParam(required=false) Long requestId,
            @RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="50") @Min(1) @Max(200) int size) {
        return ApiResponse.success("Audit history", PageView.of(service.audit(Actor.from(auth), requestId, page(page, size))));
    }
}
