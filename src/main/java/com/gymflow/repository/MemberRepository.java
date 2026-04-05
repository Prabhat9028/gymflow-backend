package com.gymflow.repository;
import com.gymflow.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByMemberCode(String memberCode);
    Optional<Member> findByDeviceUserId(String deviceUserId);
    Page<Member> findByBranchIdAndIsActiveTrue(UUID branchId, Pageable pageable);
    @Query("SELECT m FROM Member m WHERE m.branch.id = :bid AND (LOWER(m.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(m.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR m.memberCode LIKE CONCAT('%',:q,'%') OR m.phone LIKE CONCAT('%',:q,'%'))")
    Page<Member> searchByBranch(@Param("bid") UUID branchId, @Param("q") String query, Pageable pageable);
    long countByBranchIdAndIsActiveTrue(UUID branchId);
    @Query("SELECT m FROM Member m WHERE m.branch.id = :bid ORDER BY m.createdAt DESC")
    List<Member> findRecentByBranch(@Param("bid") UUID branchId, Pageable pageable);
    boolean existsByPhoneAndBranchId(String phone, UUID branchId);
}
