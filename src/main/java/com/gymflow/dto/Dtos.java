package com.gymflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class Dtos {

    // ===== AUTH =====
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token;
        private String email;
        private String role;
        private UUID userId;
    }

    // ===== MEMBER =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        private String email;
        private String phone;
        private String gender;
        private LocalDate dateOfBirth;
        private String address;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String photoUrl;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberResponse {
        private UUID id;
        private String memberCode;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String gender;
        private LocalDate dateOfBirth;
        private String address;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String photoUrl;
        private LocalDate joinDate;
        private Boolean isActive;
        private SubscriptionResponse activeSubscription;
        private Boolean hasBiometric;
    }

    // ===== PLAN =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanRequest {
        @NotBlank private String name;
        private String description;
        @NotNull private Integer durationDays;
        @NotNull private BigDecimal price;
        private List<String> features;
        private Integer maxFreezeDays;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanResponse {
        private UUID id;
        private String name;
        private String description;
        private Integer durationDays;
        private BigDecimal price;
        private List<String> features;
        private Integer maxFreezeDays;
        private Boolean isActive;
    }

    // ===== SUBSCRIPTION =====
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionRequest {
        @NotNull private UUID memberId;
        @NotNull private UUID planId;
        private LocalDate startDate;
        private BigDecimal amountPaid;
        private String paymentMethod;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubscriptionResponse {
        private UUID id;
        private UUID memberId;
        private String memberName;
        private PlanResponse plan;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private BigDecimal amountPaid;
        private long daysRemaining;
    }

    // ===== PAYMENT =====
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PaymentRequest {
        @NotNull private UUID memberId;
        private UUID subscriptionId;
        @NotNull private BigDecimal amount;
        private String paymentMethod;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentResponse {
        private UUID id;
        private UUID memberId;
        private String memberName;
        private BigDecimal amount;
        private String paymentMethod;
        private String status;
        private String transactionRef;
        private LocalDateTime paymentDate;
        private String notes;
    }

    // ===== TRAINER =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrainerRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        private String email;
        private String phone;
        private String specialization;
        private String certification;
        private BigDecimal hourlyRate;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrainerResponse {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String specialization;
        private String certification;
        private BigDecimal hourlyRate;
        private Boolean isActive;
    }

    // ===== ATTENDANCE =====
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceRequest {
        private UUID memberId;
        private String memberCode;
        private String verificationMethod;
        private String biometricTemplate;
        private BigDecimal biometricMatchScore;
        private String deviceId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttendanceResponse {
        private UUID id;
        private UUID memberId;
        private String memberName;
        private String memberCode;
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
        private String verificationMethod;
        private BigDecimal biometricMatchScore;
        private String duration;
    }

    // ===== BIOMETRIC =====
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BiometricEnrollRequest {
        @NotNull private UUID memberId;
        @NotBlank private String biometricType;
        @NotBlank private String templateData;
        private String deviceId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BiometricVerifyRequest {
        @NotBlank private String templateData;
        @NotBlank private String biometricType;
        private String deviceId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BiometricResponse {
        private UUID id;
        private UUID memberId;
        private String memberName;
        private String biometricType;
        private Boolean isActive;
        private LocalDateTime enrolledAt;
        private LocalDateTime lastVerifiedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BiometricVerifyResponse {
        private boolean matched;
        private UUID memberId;
        private String memberName;
        private String memberCode;
        private BigDecimal matchScore;
        private String membershipStatus;
        private UUID attendanceId;
    }

    // ===== CLASS =====
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GymClassRequest {
        @NotBlank private String name;
        private String description;
        private UUID trainerId;
        private String scheduleDay;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private String room;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GymClassResponse {
        private UUID id;
        private String name;
        private String description;
        private TrainerResponse trainer;
        private String scheduleDay;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private String room;
        private Boolean isActive;
    }

    // ===== DASHBOARD =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardStats {
        private long totalMembers;
        private long activeMembers;
        private long todayCheckIns;
        private long expiringThisWeek;
        private BigDecimal monthlyRevenue;
        private BigDecimal todayRevenue;
        private long activeTrainers;
        private long totalClasses;
        private List<AttendanceChartData> weeklyAttendance;
        private List<RevenueChartData> monthlyRevenueChart;
        private List<MemberResponse> recentMembers;
        private List<AttendanceResponse> recentCheckIns;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttendanceChartData {
        private String day;
        private long count;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RevenueChartData {
        private String month;
        private BigDecimal amount;
    }

    // ===== PAGINATION =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
