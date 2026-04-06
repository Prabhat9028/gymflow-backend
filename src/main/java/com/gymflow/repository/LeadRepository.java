package com.gymflow.repository;
import com.gymflow.entity.Lead;
import com.gymflow.entity.Lead.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Page<Lead> findByBranchIdOrderByCreatedAtDesc(UUID branchId, Pageable pageable);
    List<Lead> findByBranchIdAndStatus(UUID branchId, LeadStatus status);
    List<Lead> findByBranchId(UUID branchId);
    Optional<Lead> findByPhoneAndBranchId(String phone, UUID branchId);
    long countByBranchIdAndStatus(UUID branchId, LeadStatus status);
    long countByBranchId(UUID branchId);
    @Query("SELECT l FROM Lead l WHERE l.branch.id = :bid AND l.nextFollowUp <= :now AND l.status NOT IN ('CONVERTED','LOST') ORDER BY l.nextFollowUp ASC")
    List<Lead> findOverdueFollowUps(@Param("bid") UUID branchId, @Param("now") LocalDateTime now);
    @Query("SELECT l FROM Lead l WHERE l.branch.id = :bid AND (LOWER(l.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(l.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR l.phone LIKE CONCAT('%',:q,'%'))")
    Page<Lead> searchByBranch(@Param("bid") UUID branchId, @Param("q") String query, Pageable pageable);
    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE l.branch.id = :bid GROUP BY l.status")
    List<Object[]> countByStatusGrouped(@Param("bid") UUID branchId);
    @Query("SELECT l.leadSource, COUNT(l) FROM Lead l WHERE l.branch.id = :bid GROUP BY l.leadSource")
    List<Object[]> countBySourceGrouped(@Param("bid") UUID branchId);
    @Query("SELECT l.assignedTo, COUNT(l), SUM(CASE WHEN l.status = 'CONVERTED' THEN 1 ELSE 0 END) FROM Lead l WHERE l.branch.id = :bid AND l.assignedTo IS NOT NULL GROUP BY l.assignedTo")
    List<Object[]> conversionByAssignee(@Param("bid") UUID branchId);
}
