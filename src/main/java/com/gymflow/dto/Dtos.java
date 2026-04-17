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
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest { @NotBlank @Email private String email; @NotBlank private String password; }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ChangePasswordRequest { @NotBlank private String currentPassword; @NotBlank private String newPassword; }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token; private String email; private String role; private UUID userId;
        private UUID companyId; private String companyName; private String companyLogo;
        private UUID branchId; private String branchName;
        private List<BranchInfo> branches;
        private Boolean passwordChanged; private String theme;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BranchInfo { private UUID id; private String name; private String code; private String city; }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberRequest {
        @NotBlank private String firstName; @NotBlank private String lastName;
        private String email; private String phone; private String gender;
        private LocalDate dateOfBirth; private String address;
        private String emergencyContactName; private String emergencyContactPhone;
        // Plan assignment during registration
        private UUID planId;
        private BigDecimal discountAmount;
        private BigDecimal amountPaid;
        private BigDecimal balanceAmount;
        private LocalDate balanceDueDate;
        private String paymentMode; // CASH, UPI, CARD, BANK_TRANSFER
        // Additional fields
        private String source;
        private String counsellor;
        private String notes;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberResponse {
        private UUID id; private String memberCode; private String firstName; private String lastName;
        private String email; private String phone; private String gender;
        private LocalDate dateOfBirth; private String address;
        private String emergencyContactName; private String emergencyContactPhone;
        private LocalDate joinDate; private Boolean isActive;
        private String deviceUserId; private Boolean biometricEnrolled;
        private SubscriptionResponse activeMembership; private UUID branchId; private String branchName;
        private String source; private String counsellor; private String notes;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanRequest {
        @NotBlank private String name; private String description;
        @NotNull private Integer durationDays; @NotNull private BigDecimal price;
        private List<String> features; private Integer maxFreezeDays;
        private String planType; // MEMBERSHIP or PT
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanResponse {
        private UUID id; private String name; private String description;
        private Integer durationDays; private BigDecimal price;
        private List<String> features; private Integer maxFreezeDays; private Boolean isActive;
        private String planType;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionRequest {
        @NotNull private UUID memberId; @NotNull private UUID planId;
        private LocalDate startDate; private BigDecimal amountPaid; private String paymentMethod;
        private BigDecimal discountAmount; private BigDecimal balanceAmount; private LocalDate balanceDueDate;
        private UUID trainerId; // for PT subscriptions
        private String subType; // MEMBERSHIP or PT
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionEditRequest {
        private UUID planId; private LocalDate startDate; private LocalDate endDate;
        private String status; private UUID trainerId;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubscriptionResponse {
        private UUID id; private UUID memberId; private String memberName; private PlanResponse plan;
        private LocalDate startDate; private LocalDate endDate; private String status;
        private BigDecimal amountPaid; private long daysRemaining;
        private String subType; private UUID trainerId; private String trainerName;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentResponse {
        private UUID id; private UUID memberId; private String memberName; private String memberPhone;
        private BigDecimal amount; private BigDecimal discountAmount;
        private BigDecimal amountPaid; private BigDecimal balanceAmount;
        private LocalDate balanceDueDate; private String paymentMethod;
        private String status; private String transactionRef;
        private LocalDateTime paymentDate; private String planName; private BigDecimal planPrice;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CollectBalanceRequest { @NotNull private UUID paymentId; @NotNull private BigDecimal amount; private String paymentMethod; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrainerRequest { @NotBlank private String firstName; @NotBlank private String lastName; private String email; private String phone; private String specialization; private String certification; private BigDecimal hourlyRate; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrainerResponse { private UUID id; private String firstName; private String lastName; private String email; private String phone; private String specialization; private String certification; private BigDecimal hourlyRate; private Boolean isActive; }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceRequest { private UUID memberId; private String memberCode; private String verificationMethod; private String deviceId; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttendanceResponse {
        private UUID id; private UUID memberId; private String memberName; private String memberCode;
        private String memberPhone; private LocalDate subscriptionEndDate; private String subscriptionStatus;
        private LocalDateTime checkInTime; private LocalDateTime checkOutTime;
        private String verificationMethod; private String duration;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StaffRequest {
        @NotBlank private String firstName; @NotBlank private String lastName;
        private String email; private String password; private String phone; private String role;
        private String department; private String designation; private LocalDate dateOfBirth;
        private String address; private BigDecimal salary; private LocalTime shiftStart; private LocalTime shiftEnd;
        private String photoUrl;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StaffResponse {
        private UUID id; private String staffCode; private String firstName; private String lastName;
        private String email; private String phone; private String role; private String department;
        private String designation; private BigDecimal salary; private LocalDate joinDate;
        private LocalTime shiftStart; private LocalTime shiftEnd; private Boolean isActive;
        private UUID branchId; private String branchName; private String photoUrl;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StaffAttendanceRequest { private UUID staffId; private String staffCode; private String notes; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StaffAttendanceResponse {
        private UUID id; private UUID staffId; private String staffName; private String staffCode;
        private LocalDateTime checkInTime; private LocalDateTime checkOutTime;
        private LocalTime shiftStart; private LocalTime shiftEnd;
        private String status; private Integer lateMinutes; private Integer overtimeMinutes; private String duration;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardStats {
        private long totalMembers; private long activeMembers; private long todayCheckIns;
        private long expiringThisWeek; private BigDecimal monthlyRevenue; private BigDecimal todayRevenue;
        private long activeTrainers;
        private List<ChartData> expiryChart; private List<ChartData> monthlyRevenueChart;
        private List<MemberResponse> recentMembers; private List<AttendanceResponse> recentCheckIns;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChartData { private String label; private Number value; }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DeviceRequest {
        @NotBlank private String deviceSerial; @NotBlank private String deviceName;
        private String deviceIp; private Integer devicePort; private String deviceType;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MembershipReport {
        private long totalMembers; private long activeMembers; private long inactiveMembers;
        private long activeMemberships; private long expiredMemberships;
        private long frozenMemberships; private long cancelledMemberships;
        private long expiringIn7Days; private long expiringIn30Days;
        private List<PlanDistribution> planDistribution;
        private List<MemberStatusEntry> upcomingExpiry;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlanDistribution { private String planName; private long activeCount; private BigDecimal revenue; private double percentage; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberStatusEntry { private UUID memberId; private String memberName; private String memberCode; private String memberPhone; private String planName; private BigDecimal planPrice; private BigDecimal amountPaid; private LocalDate endDate; private String status; private long daysUntilExpiry; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PendingPaymentsReport { private long totalPending; private BigDecimal totalPendingAmount; private long overdue; private BigDecimal overdueAmount; private List<PendingPaymentEntry> entries; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PendingPaymentEntry { private UUID memberId; private String memberName; private String memberCode; private String memberPhone; private String planName; private BigDecimal amountDue; private BigDecimal amountPaid; private BigDecimal balance; private LocalDate dueDate; private String status; }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BiometricEnrollRequest { @NotNull private UUID memberId; private String deviceSerial; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BiometricEnrollResponse { private boolean success; private UUID memberId; private String memberName; private String deviceUserId; private String message; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DeviceResponse { private UUID id; private String deviceSerial; private String deviceName; private String deviceIp; private Integer devicePort; private String deviceType; private LocalDateTime lastHeartbeat; private Boolean isActive; private UUID branchId; private String branchName; }

    // ===== GYM (COMPANY) & BRANCH MANAGEMENT =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CompanyRequest {
        @NotBlank private String name;
        @NotBlank private String code;
        private String email;
        private String phone;
        private String address;
        private String logoUrl;
        private String theme;
        private String adminEmail;
        private String adminPassword;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CompanyResponse {
        private UUID id; private String name; private String code;
        private String email; private String phone; private String address; private String logoUrl; private String theme;
        private Boolean isActive; private LocalDateTime createdAt;
        private long branchCount; private long memberCount; private long staffCount;
        private List<BranchResponse> branches;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BranchRequest {
        @NotBlank private String name;
        @NotBlank private String code;
        private String address;
        private String city;
        private String phone;
        private String email;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BranchResponse {
        private UUID id; private UUID companyId; private String companyName;
        private String name; private String code;
        private String address; private String city; private String phone; private String email;
        private Boolean isActive; private LocalDateTime createdAt;
        private long memberCount; private long staffCount;
    }

    // ===== AI CHURN PREDICTION =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChurnPrediction {
        private UUID memberId; private String memberName; private String memberCode; private String phone;
        private String planName; private LocalDate expiryDate; private long daysToExpiry;
        private String riskLevel; // HIGH, MEDIUM, LOW
        private int riskScore; // 0-100
        private long totalVisits; private long recentVisits; private long lastMonthVisits;
        private long daysSinceLastVisit; private double visitFrequency; // visits per week
        private BigDecimal lifetimeValue; private int renewalCount;
        private String aiInsight; private String suggestedAction; private String whatsappMessage;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChurnDashboard {
        private long totalAtRisk; private long highRisk; private long mediumRisk; private long lowRisk;
        private BigDecimal revenueAtRisk; private BigDecimal highRiskRevenue;
        private List<ChurnPrediction> predictions;
        private List<ChartData> riskDistribution;
    }

    // ===== LEAD MANAGEMENT =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeadRequest {
        @NotBlank private String firstName; private String lastName;
        @NotBlank private String phone; private String email; private String gender;
        private String leadSource; private String campaignName; private String referredBy;
        private String assignedTo; private UUID assignedStaffId;
        private String interestedPlan; private LocalDate expectedJoinDate;
        private LocalDateTime nextFollowUp; private String notes;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeadResponse {
        private UUID id; private String firstName; private String lastName; private String phone; private String email; private String gender;
        private String status; private String leadSource; private String campaignName; private String referredBy;
        private String assignedTo; private String interestedPlan; private LocalDate expectedJoinDate;
        private LocalDateTime nextFollowUp; private LocalDateTime lastContacted; private Integer followUpCount;
        private String lostReason; private UUID convertedMemberId; private LocalDateTime convertedAt;
        private String notes; private LocalDateTime createdAt;
        private List<LeadActivityResponse> recentActivities;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeadStatusUpdate { @NotBlank private String status; private String notes; private String callResponse; private LocalDateTime nextFollowUp; private String performedBy; private String lostReason; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeadActivityResponse { private UUID id; private String type; private String notes; private String performedBy; private String oldStatus; private String newStatus; private String callResponse; private LocalDateTime nextFollowUp; private LocalDateTime createdAt; }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeadDashboard {
        private long total; private long newCount; private long contacted; private long followUp;
        private long trial; private long negotiation; private long converted; private long lost;
        private double conversionRate;
        private List<ChartData> statusDistribution; private List<ChartData> sourceDistribution;
        private List<CounsellorStats> counsellorStats; private List<LeadResponse> overdueFollowUps;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CounsellorStats { private String name; private long totalLeads; private long converted; private double conversionRate; }

    // ===== DIGITAL SIGNAGE =====
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SignageDeviceResponse {
        private UUID id; private String deviceCode; private String deviceName; private String deviceId;
        private String deviceModel; private String screenResolution; private String locationLabel;
        private String status; private UUID playlistId; private String playlistName;
        private LocalDateTime lastHeartbeat; private LocalDateTime lastSync;
        private String ipAddress; private String appVersion; private Boolean isActive;
        private UUID branchId; private String branchName;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SignageDeviceRequest {
        @NotBlank private String deviceName; private String locationLabel;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DevicePairRequest {
        @NotBlank private String deviceCode; @NotBlank private String deviceId;
        private String deviceModel; private String screenResolution; private String appVersion;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DeviceHeartbeatRequest {
        @NotBlank private String deviceId; private String ipAddress; private String appVersion;
        private String currentContentId; private String status;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SignageContentResponse {
        private UUID id; private String name; private String fileName; private String fileUrl;
        private String thumbnailUrl; private String contentType; private String mimeType;
        private Long fileSize; private Integer durationSeconds; private Integer width; private Integer height;
        private String checksum; private Boolean isActive; private LocalDateTime createdAt;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SignageContentRequest {
        @NotBlank private String name; private String contentType;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SignagePlaylistResponse {
        private UUID id; private String name; private String description; private String mode;
        private Boolean loopPlaylist; private Boolean scheduleEnabled;
        private String scheduleStart; private String scheduleEnd; private String scheduleDays;
        private Boolean isActive; private LocalDateTime createdAt;
        private List<PlaylistItemResponse> items; private int deviceCount;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SignagePlaylistRequest {
        @NotBlank private String name; private String description; private String mode;
        private Boolean loopPlaylist; private Boolean scheduleEnabled;
        private String scheduleStart; private String scheduleEnd; private String scheduleDays;
        private List<PlaylistItemInput> items;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlaylistItemResponse {
        private UUID id; private UUID contentId; private String contentName; private String contentType;
        private String fileUrl; private String thumbnailUrl; private String checksum;
        private Long fileSize; private Integer durationSeconds; private Integer displayDuration;
        private Integer sortOrder; private String transitionType;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PlaylistItemInput {
        @NotNull private UUID contentId; private Integer displayDuration; private String transitionType;
    }
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DeviceSyncResponse {
        private UUID playlistId; private String playlistName; private String mode; private Boolean loop;
        private long serverTimestamp; private List<PlaylistItemResponse> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PageResponse<T> { private List<T> content; private int page; private int size; private long totalElements; private int totalPages; }
}
