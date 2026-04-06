package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "leads")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;

    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name") private String lastName;
    @Column(nullable = false) private String phone;
    private String email;
    @Enumerated(EnumType.STRING) private Gender gender;

    // Lead pipeline
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    // Source tracking
    @Column(name = "lead_source") private String leadSource; // WALKIN, PHONE, SOCIAL_MEDIA, REFERENCE, WEBSITE, CAMPAIGN
    @Column(name = "campaign_name") private String campaignName;
    @Column(name = "referred_by") private String referredBy;

    // Assignment
    @Column(name = "assigned_to") private String assignedTo; // counsellor name
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_staff_id") private Staff assignedStaff;

    // Interest
    @Column(name = "interested_plan") private String interestedPlan;
    @Column(name = "expected_join_date") private LocalDate expectedJoinDate;

    // Follow-up
    @Column(name = "next_follow_up") private LocalDateTime nextFollowUp;
    @Column(name = "last_contacted") private LocalDateTime lastContacted;
    @Column(name = "follow_up_count") @Builder.Default private Integer followUpCount = 0;

    // Outcome
    @Column(name = "lost_reason") private String lostReason;
    @Column(name = "converted_member_id") private UUID convertedMemberId;
    @Column(name = "converted_at") private LocalDateTime convertedAt;

    // Notes
    @Column(length = 2000) private String notes;

    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public enum LeadStatus { NEW, CONTACTED, FOLLOW_UP, TRIAL, NEGOTIATION, CONVERTED, LOST }
    public enum Gender { MALE, FEMALE, OTHER }
}
