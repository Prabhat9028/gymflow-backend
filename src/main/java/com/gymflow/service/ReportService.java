package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ReportService {
    private final MemberRepository memberRepo;
    private final SubscriptionRepository subRepo;
    private final MembershipPlanRepository planRepo;
    private final PaymentRepository payRepo;

    public MembershipReport getMembershipReport(UUID branchId, LocalDate startDate, LocalDate endDate) {
        long totalMembers = memberRepo.countByBranchIdAndIsActiveTrue(branchId);
        long activeSubs = subRepo.countByBranchIdAndStatus(branchId, Subscription.MembershipStatus.ACTIVE);
        long expiredSubs = subRepo.countByBranchIdAndStatus(branchId, Subscription.MembershipStatus.EXPIRED);
        long frozenSubs = subRepo.countByBranchIdAndStatus(branchId, Subscription.MembershipStatus.FROZEN);
        long cancelledSubs = subRepo.countByBranchIdAndStatus(branchId, Subscription.MembershipStatus.CANCELLED);

        // Inactive = total members minus those with active subs
        long inactiveMembers = Math.max(0, totalMembers - activeSubs);

        LocalDate now = LocalDate.now();
        long exp7 = subRepo.findExpiringByBranch(branchId, now, now.plusDays(7), Subscription.MembershipStatus.ACTIVE).size();
        long exp30 = subRepo.findExpiringByBranch(branchId, now, now.plusDays(30), Subscription.MembershipStatus.ACTIVE).size();

        // Upcoming expiry list
        List<MemberStatusEntry> upcoming = subRepo.findExpiringByBranch(branchId, now, now.plusDays(30), Subscription.MembershipStatus.ACTIVE)
            .stream().map(s -> MemberStatusEntry.builder()
                .memberId(s.getMember().getId())
                .memberName(s.getMember().getFirstName() + " " + s.getMember().getLastName())
                .memberCode(s.getMember().getMemberCode())
                .planName(s.getPlan() != null ? s.getPlan().getName() : "N/A")
                .endDate(s.getEndDate()).status(s.getStatus().name())
                .daysUntilExpiry(ChronoUnit.DAYS.between(now, s.getEndDate()))
                .build())
            .sorted(Comparator.comparingLong(MemberStatusEntry::getDaysUntilExpiry))
            .toList();

        // Plan-wise distribution
        List<PlanDistribution> planDist = buildPlanDistribution(branchId);

        return MembershipReport.builder()
            .totalMembers(totalMembers).activeMembers(activeSubs).inactiveMembers(inactiveMembers)
            .activeSubscriptions(activeSubs).expiredSubscriptions(expiredSubs)
            .frozenSubscriptions(frozenSubs).cancelledSubscriptions(cancelledSubs)
            .expiringIn7Days(exp7).expiringIn30Days(exp30)
            .planDistribution(planDist).upcomingExpiry(upcoming)
            .build();
    }

    public PendingPaymentsReport getPendingPaymentsReport(UUID branchId) {
        // Use actual Payment records that have balance > 0
        List<Payment> allPayments = payRepo.findByBranchId(branchId);
        List<PendingPaymentEntry> entries = new ArrayList<>();
        BigDecimal totalPendingAmt = BigDecimal.ZERO;
        BigDecimal overdueAmt = BigDecimal.ZERO;
        long pendingCount = 0, overdueCount = 0;

        for (Payment p : allPayments) {
            BigDecimal balance = p.getBalanceAmount() != null ? p.getBalanceAmount() : BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) <= 0 && p.getStatus() == Payment.PaymentStatus.PAID) continue;

            // Check if it's overdue
            boolean isOverdue = false;
            if (p.getBalanceDueDate() != null && p.getBalanceDueDate().isBefore(LocalDate.now())) isOverdue = true;
            if (p.getDueDate() != null && p.getDueDate().isBefore(LocalDate.now())) isOverdue = true;
            if (p.getStatus() == Payment.PaymentStatus.OVERDUE) isOverdue = true;

            String status = isOverdue ? "OVERDUE" : p.getStatus().name();
            String planName = (p.getSubscription() != null && p.getSubscription().getPlan() != null)
                ? p.getSubscription().getPlan().getName() : "N/A";

            BigDecimal totalAmt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            BigDecimal paid = p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) == 0) balance = totalAmt.subtract(paid);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

            entries.add(PendingPaymentEntry.builder()
                .memberId(p.getMember().getId())
                .memberName(p.getMember().getFirstName() + " " + p.getMember().getLastName())
                .memberCode(p.getMember().getMemberCode())
                .planName(planName)
                .amountDue(totalAmt).amountPaid(paid).balance(balance)
                .dueDate(p.getBalanceDueDate() != null ? p.getBalanceDueDate() : p.getDueDate())
                .status(status).build());

            totalPendingAmt = totalPendingAmt.add(balance);
            pendingCount++;
            if (isOverdue) { overdueAmt = overdueAmt.add(balance); overdueCount++; }
        }

        return PendingPaymentsReport.builder()
            .totalPending(pendingCount).totalPendingAmount(totalPendingAmt)
            .overdue(overdueCount).overdueAmount(overdueAmt)
            .entries(entries).build();
    }

    private List<PlanDistribution> buildPlanDistribution(UUID branchId) {
        List<MembershipPlan> plans = planRepo.findByBranchIdAndIsActiveTrue(branchId);
        List<Subscription> activeSubs = subRepo.findByBranchId(branchId).stream()
            .filter(s -> s.getStatus() == Subscription.MembershipStatus.ACTIVE).toList();

        long totalActive = activeSubs.size();

        return plans.stream().map(plan -> {
            List<Subscription> planSubs = activeSubs.stream()
                .filter(s -> s.getPlan() != null && s.getPlan().getId().equals(plan.getId())).toList();
            long count = planSubs.size();
            BigDecimal revenue = planSubs.stream()
                .map(s -> s.getAmountPaid() != null ? s.getAmountPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            double pct = totalActive > 0 ? Math.round(count * 1000.0 / totalActive) / 10.0 : 0;
            return PlanDistribution.builder()
                .planName(plan.getName()).activeCount(count).revenue(revenue).percentage(pct).build();
        }).toList();
    }
}
