package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "attendance")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "check_in_time", nullable = false) private LocalDateTime checkInTime;
    @Column(name = "check_out_time") private LocalDateTime checkOutTime;
    @Column(name = "verification_method") private String verificationMethod = "BIOMETRIC";
    @Column(name = "device_id") private String deviceId;
    @Column(name = "device_log_id") private String deviceLogId;
    private String notes;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
}
