package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity @Table(name = "signage_playlists")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SignagePlaylist {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;

    @Column(nullable = false) private String name;
    private String description;

    @Enumerated(EnumType.STRING) @Builder.Default private PlaylistMode mode = PlaylistMode.SEQUENTIAL;
    @Column(name = "loop_playlist") @Builder.Default private Boolean loopPlaylist = true;

    // Scheduling
    @Column(name = "schedule_enabled") @Builder.Default private Boolean scheduleEnabled = false;
    @Column(name = "schedule_start") private LocalTime scheduleStart;
    @Column(name = "schedule_end") private LocalTime scheduleEnd;
    @Column(name = "schedule_days") private String scheduleDays; // "MON,TUE,WED,THU,FRI"

    @Column(name = "is_active") @Builder.Default private Boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public enum PlaylistMode { SEQUENTIAL, SHUFFLE }
}
