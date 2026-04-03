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
        // Find subscriptions where amountPaid < plan price
        List<Subscription> allSubs = subRepo.findByBranchId(branchId);
        List<PendingPaymentEntry> entries = new ArrayList<>();
        BigDecimal totalPendingAmt = BigDecimal.ZERO;
        BigDecimal overdueAmt = BigDecimal.ZERO;
        long pendingCount = 0, overdueCount = 0;

        for (Subscription s : allSubs) {
            if (s.getStatus() != Subscription.MembershipStatus.ACTIVE || s.getPlan() == null) continue;
            BigDecimal planPrice = s.getPlan().getPrice();
            BigDecimal paid = s.getAmountPaid() != null ? s.getAmountPaid() : BigDecimal.ZERO;

            if (paid.compareTo(planPrice) < 0) {
                BigDecimal balance = planPrice.subtract(paid);
                boolean isOverdue = s.getStartDate().plusDays(7).isBefore(LocalDate.now());
                String status = isOverdue ? "OVERDUE" : "PENDING";

                entries.add(PendingPaymentEntry.builder()
                    .memberId(s.getMember().getId())
                    .memberName(s.getMember().getFirstName() + " " + s.getMember().getLastName())
                    .memberCode(s.getMember().getMemberCode())
                    .planName(s.getPlan().getName())
                    .amountDue(planPrice).amountPaid(paid).balance(balance)
                    .dueDate(s.getStartDate().plusDays(7))
                    .status(status).build());

                totalPendingAmt = totalPendingAmt.add(balance);
                pendingCount++;
                if (isOverdue) { overdueAmt = overdueAmt.add(balance); overdueCount++; }
            }
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
