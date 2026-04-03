package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity @Table(name = "staff_attendance")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffAttendance {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "staff_id", nullable = false) private Staff staff;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "check_in_time", nullable = false) private LocalDateTime checkInTime;
    @Column(name = "check_out_time") private LocalDateTime checkOutTime;
    @Column(name = "shift_start") private LocalTime shiftStart;
    @Column(name = "shift_end") private LocalTime shiftEnd;
    private String status = "PRESENT";
    @Column(name = "late_minutes") private Integer lateMinutes = 0;
    @Column(name = "overtime_minutes") private Integer overtimeMinutes = 0;
    private String notes;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
}
