package com.gymflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "biometric_data")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BiometricData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "biometric_type", nullable = false)
    private BiometricType biometricType;

    @Column(name = "template_data", nullable = false, columnDefinition = "TEXT")
    private String templateData;

    @Column(name = "template_hash")
    private String templateHash;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public enum BiometricType {
        FINGERPRINT, FACE
    }
}
