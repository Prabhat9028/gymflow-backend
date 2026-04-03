package com.gymflow.repository;
import com.gymflow.entity.Subscription;
import com.gymflow.entity.Subscription.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
    @Query("SELECT s FROM Subscription s WHERE s.member.id = :mid AND s.status = :st ORDER BY s.endDate DESC")
    Optional<Subscription> findActiveMembership(@Param("mid") UUID memberId, @Param("st") MembershipStatus status);
    @Query("SELECT s FROM Subscription s WHERE s.branch.id = :bid AND s.endDate BETWEEN :start AND :end AND s.status = :st")
    List<Subscription> findExpiringByBranch(@Param("bid") UUID branchId, @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("st") MembershipStatus status);
    long countByBranchIdAndStatus(UUID branchId, MembershipStatus status);
    List<Subscription> findByBranchId(UUID branchId);
}
