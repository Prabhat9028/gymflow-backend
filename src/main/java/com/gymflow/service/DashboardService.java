package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberService memberService;
    private final AttendanceService attendanceService;
    private final SubscriptionService subscriptionService;
    private final TrainerService trainerService;

    public DashboardStats getStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);

        // Weekly attendance chart (last 7 days)
        List<AttendanceChartData> weeklyAttendance = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            long count = attendanceService.countCheckInsBetween(dayStart, dayEnd);
            weeklyAttendance.add(AttendanceChartData.builder()
                    .day(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .count(count)
                    .build());
        }

        // Monthly revenue chart (last 6 months)
        List<RevenueChartData> monthlyRevenue = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            BigDecimal revenue = subscriptionService.getMonthlyRevenue(ym.getMonthValue(), ym.getYear());
            monthlyRevenue.add(RevenueChartData.builder()
                    .month(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .amount(revenue != null ? revenue : BigDecimal.ZERO)
                    .build());
        }

        return DashboardStats.builder()
                .totalMembers(memberService.countActiveMembers())
                .activeMembers(memberService.countActiveMembers())
                .todayCheckIns(attendanceService.countTodayCheckIns())
                .expiringThisWeek(subscriptionService.getExpiringSubscriptions(7).size())
                .monthlyRevenue(subscriptionService.getRevenueBetween(startOfMonth, now))
                .todayRevenue(subscriptionService.getRevenueBetween(startOfDay, now))
                .activeTrainers(trainerService.countActive())
                .weeklyAttendance(weeklyAttendance)
                .monthlyRevenueChart(monthlyRevenue)
                .recentMembers(memberService.getRecentMembers(5))
                .recentCheckIns(attendanceService.getRecentCheckIns(10))
                .build();
    }
}
