package com.gymflow.repository;
import com.gymflow.entity.Subscription;
import com.gymflow.entity.Subscription.MembershipStatus;
import com.gymflow.entity.Subscription.SubType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    // Active MEMBERSHIP subscription for a member (null subType treated as MEMBERSHIP for backward compat)
    @Query("SELECT s FROM Subscription s WHERE s.member.id = :mid AND s.status = :st AND (s.subType = :stype OR s.subType IS NULL) ORDER BY s.endDate DESC")
    Optional<Subscription> findActiveSub(@Param("mid") UUID memberId, @Param("st") MembershipStatus status, @Param("stype") SubType subType);

    // Membership-only expiring (null subType treated as MEMBERSHIP)
    @Query("SELECT s FROM Subscription s WHERE s.branch.id = :bid AND s.endDate BETWEEN :start AND :end AND s.status = :st AND (s.subType = :stype OR s.subType IS NULL)")
    List<Subscription> findExpiringByBranchAndType(@Param("bid") UUID branchId, @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("st") MembershipStatus status, @Param("stype") SubType subType);

    // Membership-only counts (null subType treated as MEMBERSHIP)
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.branch.id = :bid AND s.status = :st AND (s.subType = :stype OR s.subType IS NULL)")
    long countByBranchAndStatusAndType(@Param("bid") UUID branchId, @Param("st") MembershipStatus status, @Param("stype") SubType subType);

    // All subs by branch (for plan distribution etc)
    List<Subscription> findByBranchId(UUID branchId);
}
