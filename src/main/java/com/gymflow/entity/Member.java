package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "members")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "member_code", unique = true, nullable = false) private String memberCode;
    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name", nullable = false) private String lastName;
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING) private Gender gender;
    @Column(name = "date_of_birth") private LocalDate dateOfBirth;
    private String address;
    @Column(name = "emergency_contact_name") private String emergencyContactName;
    @Column(name = "emergency_contact_phone") private String emergencyContactPhone;
    @Column(name = "photo_url") private String photoUrl;
    @Column(name = "device_user_id") private String deviceUserId;
    @Column(name = "biometric_enrolled") private Boolean biometricEnrolled = false;
    @Column(name = "join_date") private LocalDate joinDate;
    @Column(name = "is_active") private Boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public enum Gender { MALE, FEMALE, OTHER }
}
