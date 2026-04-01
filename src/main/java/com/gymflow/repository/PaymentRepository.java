package com.gymflow.repository;

import com.gymflow.entity.Payment;
import com.gymflow.entity.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByMemberIdOrderByPaymentDateDesc(UUID memberId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("status") PaymentStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND EXTRACT(MONTH FROM p.paymentDate) = :month AND EXTRACT(YEAR FROM p.paymentDate) = :year")
    BigDecimal sumRevenueByMonth(@Param("status") PaymentStatus status, @Param("month") int month, @Param("year") int year);

    Page<Payment> findAllByOrderByPaymentDateDesc(Pageable pageable);
}
