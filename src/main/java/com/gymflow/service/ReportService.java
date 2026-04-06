package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class ReportService {
    private final MemberRepository memberRepo;
    private final SubscriptionRepository subRepo;
    private final MembershipPlanRepository planRepo;
    private final PaymentRepository payRepo;

    private static final Subscription.SubType M = Subscription.SubType.MEMBERSHIP;

    public MembershipReport getMembershipReport(UUID branchId, LocalDate startDate, LocalDate endDate) {
        long totalMembers = memberRepo.countByBranchIdAndIsActiveTrue(branchId);
        // MEMBERSHIP ONLY counts
        long activeSubs = subRepo.countByBranchAndStatusAndType(branchId, Subscription.MembershipStatus.ACTIVE, M);
        long expiredSubs = subRepo.countByBranchAndStatusAndType(branchId, Subscription.MembershipStatus.EXPIRED, M);
        long frozenSubs = subRepo.countByBranchAndStatusAndType(branchId, Subscription.MembershipStatus.FROZEN, M);
        long cancelledSubs = subRepo.countByBranchAndStatusAndType(branchId, Subscription.MembershipStatus.CANCELLED, M);
        long inactiveMembers = Math.max(0, totalMembers - activeSubs);

        LocalDate now = LocalDate.now();
        // MEMBERSHIP ONLY expiring
        long exp7 = subRepo.findExpiringByBranchAndType(branchId, now, now.plusDays(7), Subscription.MembershipStatus.ACTIVE, M).size();
        long exp30 = subRepo.findExpiringByBranchAndType(branchId, now, now.plusDays(30), Subscription.MembershipStatus.ACTIVE, M).size();

        List<MemberStatusEntry> upcoming = subRepo.findExpiringByBranchAndType(branchId, now, now.plusDays(30), Subscription.MembershipStatus.ACTIVE, M)
            .stream().map(s -> MemberStatusEntry.builder()
                .memberId(s.getMember().getId())
                .memberName(s.getMember().getFirstName() + " " + s.getMember().getLastName())
                .memberCode(s.getMember().getMemberCode())
                .memberPhone(s.getMember().getPhone())
                .planName(s.getPlan() != null ? s.getPlan().getName() : "N/A")
                .planPrice(s.getPlan() != null ? s.getPlan().getPrice() : null)
                .amountPaid(s.getAmountPaid())
                .endDate(s.getEndDate()).status(s.getStatus().name())
                .daysUntilExpiry(ChronoUnit.DAYS.between(now, s.getEndDate()))
                .build())
            .sorted(Comparator.comparingLong(MemberStatusEntry::getDaysUntilExpiry))
            .toList();

        List<PlanDistribution> planDist = buildPlanDistribution(branchId);

        return MembershipReport.builder()
            .totalMembers(totalMembers).activeMembers(activeSubs).inactiveMembers(inactiveMembers)
            .activeMemberships(activeSubs).expiredMemberships(expiredSubs)
            .frozenMemberships(frozenSubs).cancelledMemberships(cancelledSubs)
            .expiringIn7Days(exp7).expiringIn30Days(exp30)
            .planDistribution(planDist).upcomingExpiry(upcoming)
            .build();
    }

    public PendingPaymentsReport getPendingPaymentsReport(UUID branchId) {
        List<Payment> allPayments = payRepo.findByBranchId(branchId);
        List<PendingPaymentEntry> entries = new ArrayList<>();
        BigDecimal totalPendingAmt = BigDecimal.ZERO, overdueAmt = BigDecimal.ZERO;
        long pendingCount = 0, overdueCount = 0;

        for (Payment p : allPayments) {
            BigDecimal balance = p.getBalanceAmount() != null ? p.getBalanceAmount() : BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) <= 0 && p.getStatus() == Payment.PaymentStatus.PAID) continue;

            boolean isOverdue = (p.getBalanceDueDate() != null && p.getBalanceDueDate().isBefore(LocalDate.now()))
                || (p.getDueDate() != null && p.getDueDate().isBefore(LocalDate.now()))
                || p.getStatus() == Payment.PaymentStatus.OVERDUE;

            BigDecimal totalAmt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            BigDecimal paid = p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) == 0) balance = totalAmt.subtract(paid);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

            String planName = (p.getSubscription() != null && p.getSubscription().getPlan() != null) ? p.getSubscription().getPlan().getName() : "N/A";

            entries.add(PendingPaymentEntry.builder()
                .memberId(p.getMember().getId())
                .memberName(p.getMember().getFirstName() + " " + p.getMember().getLastName())
                .memberCode(p.getMember().getMemberCode())
                .memberPhone(p.getMember().getPhone())
                .planName(planName).amountDue(totalAmt).amountPaid(paid).balance(balance)
                .dueDate(p.getBalanceDueDate() != null ? p.getBalanceDueDate() : p.getDueDate())
                .status(isOverdue ? "OVERDUE" : p.getStatus().name()).build());

            totalPendingAmt = totalPendingAmt.add(balance); pendingCount++;
            if (isOverdue) { overdueAmt = overdueAmt.add(balance); overdueCount++; }
        }

        return PendingPaymentsReport.builder()
            .totalPending(pendingCount).totalPendingAmount(totalPendingAmt)
            .overdue(overdueCount).overdueAmount(overdueAmt).entries(entries).build();
    }

    private List<PlanDistribution> buildPlanDistribution(UUID branchId) {
        // MEMBERSHIP plans only
        List<MembershipPlan> plans = planRepo.findByBranchIdAndIsActiveTrue(branchId).stream()
            .filter(p -> p.getPlanType() == null || p.getPlanType() == MembershipPlan.PlanType.MEMBERSHIP).toList();
        List<Subscription> activeSubs = subRepo.findByBranchId(branchId).stream()
            .filter(s -> s.getStatus() == Subscription.MembershipStatus.ACTIVE && (s.getSubType() == null || s.getSubType() == Subscription.SubType.MEMBERSHIP)).toList();
        long totalActive = activeSubs.size();
        return plans.stream().map(plan -> {
            List<Subscription> planSubs = activeSubs.stream().filter(s -> s.getPlan() != null && s.getPlan().getId().equals(plan.getId())).toList();
            long count = planSubs.size();
            BigDecimal revenue = planSubs.stream().map(s -> s.getAmountPaid() != null ? s.getAmountPaid() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            double pct = totalActive > 0 ? Math.round(count * 1000.0 / totalActive) / 10.0 : 0;
            return PlanDistribution.builder().planName(plan.getName()).activeCount(count).revenue(revenue).percentage(pct).build();
        }).toList();
    }
}
