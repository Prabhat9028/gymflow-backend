package com.gymflow.repository;

import com.gymflow.entity.Subscription;
import com.gymflow.entity.Subscription.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    @Query("SELECT s FROM Subscription s WHERE s.member.id = :memberId AND s.status = :status ORDER BY s.endDate DESC")
    Optional<Subscription> findActiveMembership(@Param("memberId") UUID memberId, @Param("status") MembershipStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :start AND :end AND s.status = :status")
    List<Subscription> findExpiringBetween(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("status") MembershipStatus status);

    long countByStatus(MembershipStatus status);
}
