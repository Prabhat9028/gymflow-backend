package com.gymflow.repository;
import com.gymflow.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    @Query("SELECT a FROM Attendance a WHERE a.member.id = :mid AND a.checkOutTime IS NULL ORDER BY a.checkInTime DESC")
    Optional<Attendance> findOpenCheckIn(@Param("mid") UUID memberId);
    @Query("SELECT a FROM Attendance a WHERE a.branch.id = :bid AND a.checkInTime BETWEEN :start AND :end ORDER BY a.checkInTime DESC")
    List<Attendance> findByBranchBetween(@Param("bid") UUID branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.branch.id = :bid AND a.checkInTime BETWEEN :start AND :end")
    long countByBranchBetween(@Param("bid") UUID branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Page<Attendance> findByBranchIdOrderByCheckInTimeDesc(UUID branchId, Pageable pageable);
    @Query("SELECT a FROM Attendance a WHERE a.branch.id = :bid ORDER BY a.checkInTime DESC")
    List<Attendance> findRecentByBranch(@Param("bid") UUID branchId, Pageable pageable);
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.member.id = :mid AND a.checkInTime BETWEEN :start AND :end")
    long countByMemberBetween(@Param("mid") UUID memberId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a.checkInTime FROM Attendance a WHERE a.member.id = :mid ORDER BY a.checkInTime DESC")
    List<LocalDateTime> findCheckInDatesByMember(@Param("mid") UUID memberId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.member.id = :mid")
    long countByMemberId(@Param("mid") UUID memberId);
}
