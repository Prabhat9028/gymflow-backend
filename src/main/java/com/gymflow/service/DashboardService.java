package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.Subscription;
import com.gymflow.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@Service @RequiredArgsConstructor
public class DashboardService {
    private final MemberService memberService;
    private final AttendanceService attService;
    private final SubscriptionService subService;
    private final TrainerService trainerService;
    private final SubscriptionRepository subRepo;

    public DashboardStats getStats(UUID branchId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime som = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime sod = now.withHour(0).withMinute(0).withSecond(0);
        LocalDate today = LocalDate.now();
        Subscription.SubType M = Subscription.SubType.MEMBERSHIP;

        // Expiry chart — MEMBERSHIP ONLY
        long exp90 = subRepo.findExpiringByBranchAndType(branchId, today.minusDays(90), today, Subscription.MembershipStatus.EXPIRED, M).size();
        long exp30 = subRepo.findExpiringByBranchAndType(branchId, today.minusDays(30), today, Subscription.MembershipStatus.EXPIRED, M).size();
        long exp7past = subRepo.findExpiringByBranchAndType(branchId, today.minusDays(7), today, Subscription.MembershipStatus.EXPIRED, M).size();
        long exp7future = subRepo.findExpiringByBranchAndType(branchId, today, today.plusDays(7), Subscription.MembershipStatus.ACTIVE, M).size();
        long exp30future = subRepo.findExpiringByBranchAndType(branchId, today, today.plusDays(30), Subscription.MembershipStatus.ACTIVE, M).size();

        List<ChartData> expiryChart = List.of(
            ChartData.builder().label("Expired 90d").value(exp90).build(),
            ChartData.builder().label("Expired 30d").value(exp30).build(),
            ChartData.builder().label("Expired 7d").value(exp7past).build(),
            ChartData.builder().label("Expiring 7d").value(exp7future).build(),
            ChartData.builder().label("Expiring 30d").value(exp30future).build());

        List<ChartData> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime ms = ym.atDay(1).atStartOfDay(), me = ym.atEndOfMonth().plusDays(1).atStartOfDay();
            BigDecimal rev = subService.getRevenue(branchId, ms, me);
            monthly.add(ChartData.builder().label(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).value(rev != null ? rev : BigDecimal.ZERO).build());
        }

        return DashboardStats.builder()
            .totalMembers(memberService.countActive(branchId)).activeMembers(memberService.countActive(branchId))
            .todayCheckIns(attService.countToday(branchId)).expiringThisWeek(exp7future)
            .monthlyRevenue(subService.getRevenue(branchId, som, now)).todayRevenue(subService.getRevenue(branchId, sod, now))
            .activeTrainers(trainerService.countActive(branchId)).expiryChart(expiryChart).monthlyRevenueChart(monthly)
            .recentMembers(memberService.getRecent(branchId, 5)).recentCheckIns(attService.getRecent(branchId, 10)).build();
    }
}
