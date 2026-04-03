package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
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

    public DashboardStats getStats(UUID branchId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime som = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime sod = now.withHour(0).withMinute(0).withSecond(0);
        List<ChartData> weekly = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            LocalDateTime ds = d.atStartOfDay(), de = d.plusDays(1).atStartOfDay();
            weekly.add(ChartData.builder().label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).value(attService.countBetween(branchId, ds, de)).build());
        }
        List<ChartData> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime ms = ym.atDay(1).atStartOfDay(), me = ym.atEndOfMonth().plusDays(1).atStartOfDay();
            BigDecimal rev = subService.getRevenue(branchId, ms, me);
            monthly.add(ChartData.builder().label(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).value(rev != null ? rev : BigDecimal.ZERO).build());
        }
        return DashboardStats.builder().totalMembers(memberService.countActive(branchId)).activeMembers(memberService.countActive(branchId))
            .todayCheckIns(attService.countToday(branchId)).expiringThisWeek(subService.getExpiring(branchId, 7).size())
            .monthlyRevenue(subService.getRevenue(branchId, som, now)).todayRevenue(subService.getRevenue(branchId, sod, now))
            .activeTrainers(trainerService.countActive(branchId)).weeklyAttendance(weekly).monthlyRevenueChart(monthly)
            .recentMembers(memberService.getRecent(branchId, 5)).recentCheckIns(attService.getRecent(branchId, 10)).build();
    }
}
