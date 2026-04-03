package com.gymflow.repository;
import com.gymflow.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findByStaffCode(String code);
    Page<Staff> findByBranchId(UUID branchId, Pageable pageable);
    @Query("SELECT s FROM Staff s WHERE s.branch.id = :bid AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR s.staffCode LIKE CONCAT('%',:q,'%'))")
    Page<Staff> searchByBranch(@Param("bid") UUID branchId, @Param("q") String query, Pageable pageable);
    long countByBranchIdAndIsActiveTrue(UUID branchId);
}
