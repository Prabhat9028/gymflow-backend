package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService svc;
    @GetMapping("/plans") public ResponseEntity<List<PlanResponse>> plans(@RequestParam UUID branchId) { return ResponseEntity.ok(svc.getPlans(branchId)); }
    @PostMapping("/plans") public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest req, @RequestParam UUID branchId, @RequestParam UUID companyId) { return ResponseEntity.ok(svc.createPlan(req, branchId, companyId)); }
    @PostMapping("/subscriptions") public ResponseEntity<SubscriptionResponse> createSub(@Valid @RequestBody SubscriptionRequest req) { return ResponseEntity.ok(svc.createSubscription(req)); }
    @GetMapping("/subscriptions/member/{mid}") public ResponseEntity<List<SubscriptionResponse>> memberSubs(@PathVariable UUID mid) { return ResponseEntity.ok(svc.getMemberSubs(mid)); }
    @GetMapping("/subscriptions/expiring") public ResponseEntity<List<SubscriptionResponse>> expiring(@RequestParam UUID branchId, @RequestParam(defaultValue="7") int days) { return ResponseEntity.ok(svc.getExpiring(branchId, days)); }
    @GetMapping("/payments") public ResponseEntity<PageResponse<PaymentResponse>> payments(@RequestParam UUID branchId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(svc.getPayments(branchId, page, size)); }
}
