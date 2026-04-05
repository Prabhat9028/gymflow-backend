package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity @Table(name = "staff")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Staff {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "staff_code", unique = true, nullable = false) private String staffCode;
    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name", nullable = false) private String lastName;
    private String email;
    private String phone;
    @Column(nullable = false) private String role;
    private String department;
    private String designation;
    @Column(name = "date_of_birth") private LocalDate dateOfBirth;
    private String address;
    private BigDecimal salary;
    @Column(name = "join_date") private LocalDate joinDate;
    @Column(name = "shift_start") private LocalTime shiftStart;
    @Column(name = "shift_end") private LocalTime shiftEnd;
    @Column(name = "photo_url") private String photoUrl;
    @Column(name = "is_active") private Boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
