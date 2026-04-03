package com.gymflow.repository;
import com.gymflow.entity.Payment;
import com.gymflow.entity.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByBranchIdOrderByPaymentDateDesc(UUID branchId, Pageable pageable);
    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.branch.id = :bid AND p.status = :st AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal sumRevenueByBranch(@Param("bid") UUID branchId, @Param("st") PaymentStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    java.util.List<Payment> findByBranchIdAndStatusIn(UUID branchId, java.util.List<PaymentStatus> statuses);
    java.util.List<Payment> findByBranchId(UUID branchId);
}
