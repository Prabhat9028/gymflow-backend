package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "biometric_devices")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BiometricDevice {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "device_serial", unique = true, nullable = false) private String deviceSerial;
    @Column(name = "device_name") private String deviceName;
    @Column(name = "device_ip") private String deviceIp;
    @Column(name = "device_port") private Integer devicePort;
    @Column(name = "device_type") private String deviceType;
    @Column(name = "last_heartbeat") private LocalDateTime lastHeartbeat;
    @Column(name = "is_active") private Boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
}
