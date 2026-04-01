package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final PaymentRepository paymentRepository;

    // ===== PLANS =====
    public List<PlanResponse> getAllPlans() {
        return planRepository.findByIsActiveTrue().stream().map(this::toPlanResponse).toList();
    }

    public PlanResponse createPlan(PlanRequest req) {
        MembershipPlan plan = MembershipPlan.builder()
                .name(req.getName())
                .description(req.getDescription())
                .durationDays(req.getDurationDays())
                .price(req.getPrice())
                .features(req.getFeatures())
                .maxFreezeDays(req.getMaxFreezeDays() != null ? req.getMaxFreezeDays() : 0)
                .isActive(true)
                .build();
        return toPlanResponse(planRepository.save(plan));
    }

    public PlanResponse updatePlan(UUID id, PlanRequest req) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        if (req.getName() != null) plan.setName(req.getName());
        if (req.getDescription() != null) plan.setDescription(req.getDescription());
        if (req.getDurationDays() != null) plan.setDurationDays(req.getDurationDays());
        if (req.getPrice() != null) plan.setPrice(req.getPrice());
        if (req.getFeatures() != null) plan.setFeatures(req.getFeatures());
        if (req.getMaxFreezeDays() != null) plan.setMaxFreezeDays(req.getMaxFreezeDays());
        return toPlanResponse(planRepository.save(plan));
    }

    public void deletePlan(UUID id) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        plan.setIsActive(false);
        planRepository.save(plan);
    }

    // ===== SUBSCRIPTIONS =====
    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest req) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
        MembershipPlan plan = planRepository.findById(req.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());

        // Expire any current active subscription
        subscriptionRepository.findActiveMembership(member.getId(), Subscription.MembershipStatus.ACTIVE).ifPresent(s -> {
            s.setStatus(Subscription.MembershipStatus.EXPIRED);
            subscriptionRepository.save(s);
        });

        Subscription sub = Subscription.builder()
                .member(member)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(Subscription.MembershipStatus.ACTIVE)
                .amountPaid(req.getAmountPaid() != null ? req.getAmountPaid() : plan.getPrice())
                .build();

        sub = subscriptionRepository.save(sub);

        // Create payment record
        Payment payment = Payment.builder()
                .member(member)
                .subscription(sub)
                .amount(sub.getAmountPaid())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
                .status(Payment.PaymentStatus.PAID)
                .transactionRef("TXN" + System.currentTimeMillis())
                .paymentDate(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        return toSubResponse(sub);
    }

    public List<SubscriptionResponse> getMemberSubscriptions(UUID memberId) {
        return subscriptionRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(this::toSubResponse).toList();
    }

    public List<SubscriptionResponse> getExpiringSubscriptions(int days) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);
        return subscriptionRepository.findExpiringBetween(start, end, Subscription.MembershipStatus.ACTIVE)
                .stream().map(this::toSubResponse).toList();
    }

    // ===== PAYMENTS =====
    public PageResponse<PaymentResponse> getAllPayments(int page, int size) {
        var pg = paymentRepository.findAllByOrderByPaymentDateDesc(
                org.springframework.data.domain.PageRequest.of(page, size));
        List<PaymentResponse> content = pg.getContent().stream().map(this::toPaymentResponse).toList();
        return PageResponse.<PaymentResponse>builder()
                .content(content).page(pg.getNumber()).size(pg.getSize())
                .totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages())
                .build();
    }

    public BigDecimal getMonthlyRevenue(int month, int year) {
        return paymentRepository.sumRevenueByMonth(Payment.PaymentStatus.PAID, month, year);
    }

    public BigDecimal getRevenueBetween(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.sumRevenueBetween(Payment.PaymentStatus.PAID, start, end);
    }

    private PlanResponse toPlanResponse(MembershipPlan p) {
        return PlanResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .durationDays(p.getDurationDays()).price(p.getPrice())
                .features(p.getFeatures()).maxFreezeDays(p.getMaxFreezeDays())
                .isActive(p.getIsActive())
                .build();
    }

    private SubscriptionResponse toSubResponse(Subscription s) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate());
        return SubscriptionResponse.builder()
                .id(s.getId())
                .memberId(s.getMember().getId())
                .memberName(s.getMember().getFirstName() + " " + s.getMember().getLastName())
                .plan(s.getPlan() != null ? toPlanResponse(s.getPlan()) : null)
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus().name())
                .amountPaid(s.getAmountPaid())
                .daysRemaining(Math.max(0, daysRemaining))
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .memberId(p.getMember().getId())
                .memberName(p.getMember().getFirstName() + " " + p.getMember().getLastName())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus().name())
                .transactionRef(p.getTransactionRef())
                .paymentDate(p.getPaymentDate())
                .notes(p.getNotes())
                .build();
    }
}
