package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChurnPredictionService {

    private final MemberRepository memberRepo;
    private final AttendanceRepository attRepo;
    private final SubscriptionRepository subRepo;
    private final PaymentRepository payRepo;
    private final MembershipPlanRepository planRepo;

    /**
     * Generate churn predictions for all active members in a branch
     */
    public ChurnDashboard predictChurn(UUID branchId) {
        List<Member> members = memberRepo.findByBranchIdAndIsActiveTrue(branchId, PageRequest.of(0, 10000)).getContent();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<ChurnPrediction> predictions = new ArrayList<>();
        BigDecimal totalRevenueAtRisk = BigDecimal.ZERO;
        BigDecimal highRiskRevenue = BigDecimal.ZERO;
        long highCount = 0, medCount = 0, lowCount = 0;

        for (Member m : members) {
            try {
                ChurnPrediction cp = analyzeMember(m, now, today, branchId);
                if (cp != null) {
                    predictions.add(cp);
                    BigDecimal planPrice = cp.getLifetimeValue() != null ? cp.getLifetimeValue() : BigDecimal.ZERO;

                    switch (cp.getRiskLevel()) {
                        case "HIGH" -> { highCount++; totalRevenueAtRisk = totalRevenueAtRisk.add(planPrice); highRiskRevenue = highRiskRevenue.add(planPrice); }
                        case "MEDIUM" -> { medCount++; totalRevenueAtRisk = totalRevenueAtRisk.add(planPrice); }
                        case "LOW" -> lowCount++;
                    }
                }
            } catch (Exception e) {
                log.debug("Churn analysis failed for member {}: {}", m.getMemberCode(), e.getMessage());
            }
        }

        // Sort: HIGH risk first, then by risk score descending
        predictions.sort((a, b) -> {
            int rl = riskOrder(a.getRiskLevel()) - riskOrder(b.getRiskLevel());
            return rl != 0 ? rl : Integer.compare(b.getRiskScore(), a.getRiskScore());
        });

        List<ChartData> dist = List.of(
            ChartData.builder().label("High Risk").value(highCount).build(),
            ChartData.builder().label("Medium Risk").value(medCount).build(),
            ChartData.builder().label("Low Risk").value(lowCount).build()
        );

        return ChurnDashboard.builder()
            .totalAtRisk(highCount + medCount)
            .highRisk(highCount).mediumRisk(medCount).lowRisk(lowCount)
            .revenueAtRisk(totalRevenueAtRisk).highRiskRevenue(highRiskRevenue)
            .predictions(predictions).riskDistribution(dist)
            .build();
    }

    /**
     * Analyze a single member's churn risk
     */
    private ChurnPrediction analyzeMember(Member m, LocalDateTime now, LocalDate today, UUID branchId) {
        // Get active membership
        var activeSub = subRepo.findActiveSub(m.getId(), Subscription.MembershipStatus.ACTIVE, Subscription.SubType.MEMBERSHIP);
        if (activeSub.isEmpty()) return null; // No active membership — already churned

        Subscription sub = activeSub.get();
        long daysToExpiry = ChronoUnit.DAYS.between(today, sub.getEndDate());

        // Skip if expiry is more than 90 days away (very unlikely to churn)
        if (daysToExpiry > 90) return null;

        // === DATA COLLECTION ===
        long totalVisits = attRepo.countByMemberId(m.getId());
        long last30Visits = attRepo.countByMemberBetween(m.getId(), now.minusDays(30), now);
        long last14Visits = attRepo.countByMemberBetween(m.getId(), now.minusDays(14), now);
        long last7Visits = attRepo.countByMemberBetween(m.getId(), now.minusDays(7), now);

        // Last visit date
        var recentDates = attRepo.findCheckInDatesByMember(m.getId(), PageRequest.of(0, 1));
        long daysSinceLastVisit = recentDates.isEmpty() ? 999 : ChronoUnit.DAYS.between(recentDates.get(0).toLocalDate(), today);

        // Visit frequency (per week) over membership duration
        long membershipDays = ChronoUnit.DAYS.between(sub.getStartDate(), today);
        double weeksActive = Math.max(1, membershipDays / 7.0);
        double visitFreq = totalVisits / weeksActive;

        // Renewal history
        List<Subscription> allSubs = subRepo.findByMemberIdOrderByCreatedAtDesc(m.getId());
        int renewalCount = (int) allSubs.stream()
            .filter(s -> s.getSubType() == null || s.getSubType() == Subscription.SubType.MEMBERSHIP)
            .count() - 1; // exclude current

        // Lifetime value
        BigDecimal ltv = allSubs.stream()
            .filter(s -> s.getAmountPaid() != null)
            .map(Subscription::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Plan price for revenue-at-risk
        BigDecimal planPrice = sub.getPlan() != null ? sub.getPlan().getPrice() : BigDecimal.ZERO;

        // === RISK SCORING (0-100) ===
        int score = 0;

        // Factor 1: Days since last visit (max 30 points)
        if (daysSinceLastVisit >= 30) score += 30;
        else if (daysSinceLastVisit >= 14) score += 20;
        else if (daysSinceLastVisit >= 7) score += 10;
        else if (daysSinceLastVisit >= 3) score += 5;

        // Factor 2: Visit frequency decline (max 25 points)
        // Compare last 14 days vs the 14 days before that
        long prev14Visits = attRepo.countByMemberBetween(m.getId(), now.minusDays(28), now.minusDays(14));
        if (prev14Visits > 0 && last14Visits == 0) score += 25;
        else if (prev14Visits > 0 && last14Visits < prev14Visits * 0.5) score += 18;
        else if (prev14Visits > 0 && last14Visits < prev14Visits * 0.75) score += 10;
        else if (last14Visits == 0 && prev14Visits == 0) score += 15; // Never comes

        // Factor 3: Days to expiry (max 20 points)
        if (daysToExpiry <= 0) score += 20; // Already expired
        else if (daysToExpiry <= 3) score += 15;
        else if (daysToExpiry <= 7) score += 10;
        else if (daysToExpiry <= 14) score += 5;

        // Factor 4: Low overall engagement (max 15 points)
        if (visitFreq < 0.5) score += 15;       // Less than once every 2 weeks
        else if (visitFreq < 1.0) score += 10;   // Less than once a week
        else if (visitFreq < 2.0) score += 5;    // Less than twice a week

        // Factor 5: No renewal history (max 10 points)
        if (renewalCount == 0) score += 10;       // First-time member
        else if (renewalCount == 1) score += 3;

        // Bonus: Ghost member (enrolled but never visited)
        if (totalVisits == 0) score = Math.min(95, score + 20);

        // Cap at 100
        score = Math.min(100, score);

        // === RISK CLASSIFICATION ===
        String riskLevel;
        if (score >= 60) riskLevel = "HIGH";
        else if (score >= 35) riskLevel = "MEDIUM";
        else riskLevel = "LOW";

        // === AI INSIGHTS ===
        String insight = generateInsight(m, daysToExpiry, daysSinceLastVisit, last30Visits, visitFreq, renewalCount, totalVisits);
        String action = generateAction(riskLevel, daysToExpiry, daysSinceLastVisit, last30Visits, renewalCount, planPrice);
        String whatsapp = generateWhatsAppMessage(m, sub, daysToExpiry, daysSinceLastVisit, riskLevel, planPrice);

        return ChurnPrediction.builder()
            .memberId(m.getId()).memberName(m.getFirstName() + " " + m.getLastName())
            .memberCode(m.getMemberCode()).phone(m.getPhone())
            .planName(sub.getPlan() != null ? sub.getPlan().getName() : "N/A")
            .expiryDate(sub.getEndDate()).daysToExpiry(daysToExpiry)
            .riskLevel(riskLevel).riskScore(score)
            .totalVisits(totalVisits).recentVisits(last14Visits).lastMonthVisits(last30Visits)
            .daysSinceLastVisit(daysSinceLastVisit == 999 ? -1 : daysSinceLastVisit)
            .visitFrequency(Math.round(visitFreq * 10.0) / 10.0)
            .lifetimeValue(planPrice).renewalCount(renewalCount)
            .aiInsight(insight).suggestedAction(action).whatsappMessage(whatsapp)
            .build();
    }

    private String generateInsight(Member m, long daysToExpiry, long daysSinceLastVisit, long last30, double freq, int renewals, long total) {
        StringBuilder sb = new StringBuilder();
        String name = m.getFirstName();

        if (total == 0) {
            sb.append(name).append(" enrolled but has NEVER visited the gym. ");
        } else if (daysSinceLastVisit > 21) {
            sb.append(name).append(" hasn't visited in ").append(daysSinceLastVisit).append(" days — likely disengaged. ");
        } else if (daysSinceLastVisit > 7) {
            sb.append(name).append("'s last visit was ").append(daysSinceLastVisit).append(" days ago — attendance dropping. ");
        }

        if (daysToExpiry <= 0) sb.append("Membership has EXPIRED. ");
        else if (daysToExpiry <= 7) sb.append("Membership expires in ").append(daysToExpiry).append(" days! ");
        else if (daysToExpiry <= 30) sb.append("Membership expiring soon (").append(daysToExpiry).append("d). ");

        if (freq < 1.0 && total > 0) sb.append("Very low visit frequency (").append(String.format("%.1f", freq)).append("/week). ");
        else if (freq >= 4.0) sb.append("Highly engaged (").append(String.format("%.1f", freq)).append(" visits/week). ");

        if (renewals >= 2) sb.append("Loyal member — ").append(renewals).append(" previous renewals. ");
        else if (renewals == 0) sb.append("First-time member — higher churn risk. ");

        return sb.toString().trim();
    }

    private String generateAction(String risk, long daysToExpiry, long daysSinceLastVisit, long last30, int renewals, BigDecimal price) {
        if ("HIGH".equals(risk)) {
            if (daysSinceLastVisit > 21) return "URGENT: Personal call from manager. Offer complimentary PT session to re-engage. Consider 10-15% discount on renewal.";
            if (daysToExpiry <= 0) return "EXPIRED: Call immediately. Offer same-plan renewal with ₹" + price.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.CEILING) + " discount if they renew within 48 hours.";
            if (daysToExpiry <= 7) return "CRITICAL: WhatsApp + call today. Offer early renewal discount. Emphasize their progress and community.";
            return "HIGH RISK: Schedule in-person conversation during their next visit. Understand barriers and offer flexible plan.";
        } else if ("MEDIUM".equals(risk)) {
            if (daysToExpiry <= 14) return "Send WhatsApp renewal reminder. Highlight benefits they use most. Follow up with call in 2 days if no response.";
            return "Monitor closely. Send friendly check-in message. Invite to a group class or event to boost engagement.";
        } else {
            return "Low risk — continue regular engagement. Send renewal reminder 7 days before expiry.";
        }
    }

    private String generateWhatsAppMessage(Member m, Subscription sub, long daysToExpiry, long daysSinceLastVisit, String risk, BigDecimal price) {
        String name = m.getFirstName();
        String plan = sub.getPlan() != null ? sub.getPlan().getName() : "membership";
        String priceStr = "₹" + (price != null ? price.setScale(0, RoundingMode.CEILING).toPlainString() : "");

        if (daysToExpiry <= 0) {
            // Expired
            return String.format("Hi %s! 🏋️ We miss you at MaxOut! Your %s plan has expired. " +
                "Renew today and get back to your fitness journey! " +
                "As a special offer, renew within 48 hours for a discount. " +
                "Reply to this message or visit us. 💪", name, plan);
        } else if (daysToExpiry <= 7) {
            // Expiring very soon
            return String.format("Hi %s! 🏋️ Your %s plan at MaxOut expires in %d days (on %s). " +
                "Renew now to keep your streak going! %s for another term. " +
                "Visit the front desk or reply here. 💪", name, plan, daysToExpiry, sub.getEndDate(), priceStr);
        } else if (daysSinceLastVisit > 14 && "HIGH".equals(risk)) {
            // Disengaged
            return String.format("Hi %s! 👋 We haven't seen you at MaxOut in %d days. " +
                "Everything okay? We'd love to help you get back on track! " +
                "Come in for a complimentary session this week — no pressure, just support. 🏋️", name, daysSinceLastVisit);
        } else if (daysToExpiry <= 30) {
            // Normal renewal reminder
            return String.format("Hi %s! 🏋️ Quick reminder — your %s plan at MaxOut expires on %s (%d days). " +
                "Renew early and never miss a workout! " +
                "See you at the gym! 💪", name, plan, sub.getEndDate(), daysToExpiry);
        } else {
            return String.format("Hi %s! Hope you're crushing your fitness goals at MaxOut! 💪 " +
                "Your %s plan is active until %s. Keep up the great work! 🏋️", name, plan, sub.getEndDate());
        }
    }

    private int riskOrder(String level) {
        return switch (level) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}
