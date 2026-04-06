package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subRepo;
    private final MemberRepository memberRepo;
    private final MembershipPlanRepository planRepo;
    private final PaymentRepository payRepo;
    private final TrainerRepository trainerRepo;

    public List<PlanResponse> getPlans(UUID branchId) { return planRepo.findByBranchIdAndIsActiveTrue(branchId).stream().map(this::toPlan).toList(); }

    public PlanResponse createPlan(PlanRequest req, UUID branchId, UUID companyId) {
        MembershipPlan.PlanType pt = MembershipPlan.PlanType.MEMBERSHIP;
        if (req.getPlanType() != null && req.getPlanType().equalsIgnoreCase("PT")) pt = MembershipPlan.PlanType.PT;
        MembershipPlan p = MembershipPlan.builder().name(req.getName()).description(req.getDescription())
            .durationDays(req.getDurationDays()).price(req.getPrice()).features(req.getFeatures())
            .maxFreezeDays(req.getMaxFreezeDays() != null ? req.getMaxFreezeDays() : 0).isActive(true)
            .planType(pt)
            .branch(new Branch(){{ setId(branchId); }}).company(new Company(){{ setId(companyId); }}).build();
        return toPlan(planRepo.save(p));
    }

    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest req) {
        Member m = memberRepo.findById(req.getMemberId()).orElseThrow(() -> new RuntimeException("Member not found"));
        MembershipPlan plan = planRepo.findById(req.getPlanId()).orElseThrow(() -> new RuntimeException("Plan not found"));
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();

        Subscription.SubType subType = Subscription.SubType.MEMBERSHIP;
        if (req.getSubType() != null && req.getSubType().equalsIgnoreCase("PT")) subType = Subscription.SubType.PT;
        else if (plan.getPlanType() == MembershipPlan.PlanType.PT) subType = Subscription.SubType.PT;

        // Expire old active subscription of same type
        List<Subscription> subs = subRepo.findByMemberIdOrderByCreatedAtDesc(m.getId());

        for (Subscription s : subs) {
            if (s.getStatus() == Subscription.MembershipStatus.ACTIVE
                    && s.getSubType() == subType) {
                s.setStatus(Subscription.MembershipStatus.EXPIRED);
                subRepo.save(s);
            }
        }
        Trainer trainer = null;
        if (req.getTrainerId() != null) trainer = trainerRepo.findById(req.getTrainerId()).orElse(null);

        BigDecimal planPrice = plan.getPrice();
        BigDecimal discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmt = planPrice.subtract(discount);
        BigDecimal amtPaid = req.getAmountPaid() != null ? req.getAmountPaid() : finalAmt;
        BigDecimal balance = finalAmt.subtract(amtPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;

        Subscription sub = subRepo.save(Subscription.builder()
            .member(m).plan(plan).branch(m.getBranch()).trainer(trainer)
            .subType(subType)
            .startDate(start).endDate(start.plusDays(plan.getDurationDays()))
            .status(Subscription.MembershipStatus.ACTIVE)
            .amountPaid(amtPaid).build());

        Payment.PaymentStatus payStatus = balance.compareTo(BigDecimal.ZERO) > 0 ? Payment.PaymentStatus.PENDING : Payment.PaymentStatus.PAID;
        payRepo.save(Payment.builder().member(m).subscription(sub).branch(m.getBranch())
            .amount(finalAmt).discountAmount(discount).amountPaid(amtPaid).balanceAmount(balance)
            .balanceDueDate(req.getBalanceDueDate())
            .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
            .status(payStatus).transactionRef("TXN" + System.currentTimeMillis())
            .paymentDate(LocalDateTime.now()).build());

        return toSub(sub);
    }

    @Transactional
    public SubscriptionResponse editSubscription(UUID subId, SubscriptionEditRequest req) {
        Subscription sub = subRepo.findById(subId).orElseThrow(() -> new RuntimeException("Subscription not found"));
        if (req.getPlanId() != null) sub.setPlan(planRepo.findById(req.getPlanId()).orElseThrow());
        if (req.getStartDate() != null) sub.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) sub.setEndDate(req.getEndDate());
        if (req.getStatus() != null) sub.setStatus(Subscription.MembershipStatus.valueOf(req.getStatus()));
        if (req.getTrainerId() != null) sub.setTrainer(trainerRepo.findById(req.getTrainerId()).orElse(null));
        return toSub(subRepo.save(sub));
    }

    public List<SubscriptionResponse> getMemberSubs(UUID memberId) { return subRepo.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(this::toSub).toList(); }
    public List<SubscriptionResponse> getExpiring(UUID branchId, int days) {
        return subRepo.findExpiringByBranchAndType(branchId, LocalDate.now(), LocalDate.now().plusDays(days), Subscription.MembershipStatus.ACTIVE, Subscription.SubType.MEMBERSHIP).stream().map(this::toSub).toList();
    }
    public PageResponse<PaymentResponse> getPayments(UUID branchId, int page, int size, LocalDate from, LocalDate to) {
        var pg = payRepo.findByBranchIdOrderByPaymentDateDesc(branchId, PageRequest.of(page, size));
        // Filter by date if provided
        var content = pg.getContent().stream().map(this::toPay);
        if (from != null) content = content.filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().toLocalDate().isBefore(from));
        if (to != null) content = content.filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().toLocalDate().isAfter(to));
        var list = content.toList();
        return PageResponse.<PaymentResponse>builder().content(list)
            .page(pg.getNumber()).size(pg.getSize()).totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages()).build();
    }
    public BigDecimal getRevenue(UUID branchId, LocalDateTime start, LocalDateTime end) { return payRepo.sumRevenueByBranch(branchId, Payment.PaymentStatus.PAID, start, end); }

    @Transactional
    public PaymentResponse collectBalance(CollectBalanceRequest req) {
        Payment p = payRepo.findById(req.getPaymentId()).orElseThrow(() -> new RuntimeException("Payment not found"));
        BigDecimal currentBalance = p.getBalanceAmount() != null ? p.getBalanceAmount() : BigDecimal.ZERO;
        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("No balance due");
        BigDecimal collecting = req.getAmount();
        if (collecting.compareTo(currentBalance) > 0) collecting = currentBalance;
        BigDecimal newPaid = (p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO).add(collecting);
        BigDecimal newBalance = currentBalance.subtract(collecting);
        p.setAmountPaid(newPaid); p.setBalanceAmount(newBalance);
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) { p.setStatus(Payment.PaymentStatus.PAID); p.setBalanceAmount(BigDecimal.ZERO); }
        if (req.getPaymentMethod() != null) p.setPaymentMethod(req.getPaymentMethod());
        return toPay(payRepo.save(p));
    }

    private PlanResponse toPlan(MembershipPlan p) {
        return PlanResponse.builder().id(p.getId()).name(p.getName()).description(p.getDescription())
            .durationDays(p.getDurationDays()).price(p.getPrice()).features(p.getFeatures())
            .maxFreezeDays(p.getMaxFreezeDays()).isActive(p.getIsActive())
            .planType(p.getPlanType() != null ? p.getPlanType().name() : "MEMBERSHIP").build();
    }
    private SubscriptionResponse toSub(Subscription s) {
        String trainerName = null; UUID trainerId = null;
        if (s.getTrainer() != null) { trainerId = s.getTrainer().getId(); trainerName = s.getTrainer().getFirstName() + " " + s.getTrainer().getLastName(); }
        return SubscriptionResponse.builder().id(s.getId()).memberId(s.getMember().getId())
            .memberName(s.getMember().getFirstName()+" "+s.getMember().getLastName())
            .plan(s.getPlan() != null ? toPlan(s.getPlan()) : null).startDate(s.getStartDate()).endDate(s.getEndDate()).status(s.getStatus().name())
            .amountPaid(s.getAmountPaid()).daysRemaining(Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate())))
            .subType(s.getSubType() != null ? s.getSubType().name() : "MEMBERSHIP")
            .trainerId(trainerId).trainerName(trainerName).build();
    }
    private PaymentResponse toPay(Payment p) {
        String planName = null; BigDecimal planPrice = null;
        if (p.getSubscription() != null && p.getSubscription().getPlan() != null) { planName = p.getSubscription().getPlan().getName(); planPrice = p.getSubscription().getPlan().getPrice(); }
        return PaymentResponse.builder().id(p.getId()).memberId(p.getMember().getId())
            .memberName(p.getMember().getFirstName()+" "+p.getMember().getLastName())
            .memberPhone(p.getMember().getPhone())
            .amount(p.getAmount()).discountAmount(p.getDiscountAmount())
            .amountPaid(p.getAmountPaid()).balanceAmount(p.getBalanceAmount())
            .balanceDueDate(p.getBalanceDueDate()).paymentMethod(p.getPaymentMethod())
            .status(p.getStatus().name()).transactionRef(p.getTransactionRef())
            .paymentDate(p.getPaymentDate()).planName(planName).planPrice(planPrice).build();
    }
}
