package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // Plans
    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAllPlans());
    }

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(subscriptionService.createPlan(request));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(subscriptionService.updatePlan(id, request));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        subscriptionService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    // Subscriptions
    @PostMapping("/subscriptions")
    public ResponseEntity<SubscriptionResponse> createSubscription(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.createSubscription(request));
    }

    @GetMapping("/subscriptions/member/{memberId}")
    public ResponseEntity<List<SubscriptionResponse>> getMemberSubscriptions(@PathVariable UUID memberId) {
        return ResponseEntity.ok(subscriptionService.getMemberSubscriptions(memberId));
    }

    @GetMapping("/subscriptions/expiring")
    public ResponseEntity<List<SubscriptionResponse>> getExpiring(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(subscriptionService.getExpiringSubscriptions(days));
    }

    // Payments
    @GetMapping("/payments")
    public ResponseEntity<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(subscriptionService.getAllPayments(page, size));
    }
}
