package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subscription_id") private Subscription subscription;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "discount_amount") private BigDecimal discountAmount;
    @Column(name = "amount_paid") private BigDecimal amountPaid;
    @Column(name = "balance_amount") private BigDecimal balanceAmount;
    @Column(name = "balance_due_date") private LocalDate balanceDueDate;
    @Column(name = "payment_method") private String paymentMethod = "CASH";
    @Enumerated(EnumType.STRING) private PaymentStatus status = PaymentStatus.PAID;
    @Column(name = "transaction_ref") private String transactionRef;
    @Column(name = "payment_date") private LocalDateTime paymentDate = LocalDateTime.now();
    @Column(name = "due_date") private LocalDate dueDate;
    private String notes;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    public enum PaymentStatus { PAID, PENDING, OVERDUE, REFUNDED }
}
