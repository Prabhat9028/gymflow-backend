package com.gymflow.repository;
import com.gymflow.entity.StaffAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, UUID> {
    @Query("SELECT sa FROM StaffAttendance sa WHERE sa.staff.id = :sid AND sa.checkOutTime IS NULL ORDER BY sa.checkInTime DESC")
    Optional<StaffAttendance> findOpenCheckIn(@Param("sid") UUID staffId);
    @Query("SELECT sa FROM StaffAttendance sa WHERE sa.branch.id = :bid AND sa.checkInTime BETWEEN :start AND :end ORDER BY sa.checkInTime DESC")
    List<StaffAttendance> findByBranchBetween(@Param("bid") UUID branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Page<StaffAttendance> findByBranchIdOrderByCheckInTimeDesc(UUID branchId, Pageable pageable);
}
