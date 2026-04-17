package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "signage_devices")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SignageDevice {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;

    @Column(name = "device_code", unique = true, nullable = false) private String deviceCode; // 6-char pairing code
    @Column(name = "device_name") private String deviceName;
    @Column(name = "device_id") private String deviceId; // Android device unique ID
    @Column(name = "device_model") private String deviceModel;
    @Column(name = "screen_resolution") private String screenResolution;
    @Column(name = "location_label") private String locationLabel; // e.g., "Reception TV", "Cardio Zone"

    @Enumerated(EnumType.STRING) @Builder.Default private DeviceStatus status = DeviceStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "playlist_id") private SignagePlaylist activePlaylist;

    @Column(name = "last_heartbeat") private LocalDateTime lastHeartbeat;
    @Column(name = "last_sync") private LocalDateTime lastSync;
    @Column(name = "ip_address") private String ipAddress;
    @Column(name = "app_version") private String appVersion;
    @Column(name = "is_active") @Builder.Default private Boolean isActive = true;

    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public enum DeviceStatus { PENDING, PAIRED, ONLINE, OFFLINE, ERROR }
}
