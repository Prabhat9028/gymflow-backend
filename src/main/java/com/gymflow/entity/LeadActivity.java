package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "lead_activities")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadActivity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id", nullable = false) private Lead lead;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityType type;
    @Column(length = 1000) private String notes;
    @Column(name = "performed_by") private String performedBy;
    @Column(name = "old_status") private String oldStatus;
    @Column(name = "new_status") private String newStatus;
    @Column(name = "next_follow_up") private LocalDateTime nextFollowUp;
    @Column(name = "call_response") private String callResponse; // INTERESTED, NOT_INTERESTED, BUSY, NO_ANSWER, CALLBACK, WRONG_NUMBER
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;

    public enum ActivityType { CALL, WHATSAPP, EMAIL, VISIT, TRIAL_SCHEDULED, TRIAL_COMPLETED, STATUS_CHANGE, NOTE, FOLLOW_UP }
}
