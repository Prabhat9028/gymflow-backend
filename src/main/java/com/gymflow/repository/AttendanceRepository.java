package com.gymflow.repository;

import com.gymflow.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    @Query("SELECT a FROM Attendance a WHERE a.member.id = :memberId AND a.checkOutTime IS NULL ORDER BY a.checkInTime DESC")
    Optional<Attendance> findOpenCheckIn(@Param("memberId") UUID memberId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.checkInTime BETWEEN :start AND :end")
    long countCheckInsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Attendance a ORDER BY a.checkInTime DESC")
    List<Attendance> findRecentCheckIns(Pageable pageable);

    Page<Attendance> findByMemberIdOrderByCheckInTimeDesc(UUID memberId, Pageable pageable);
    Page<Attendance> findAllByOrderByCheckInTimeDesc(Pageable pageable);

    @Query("SELECT a FROM Attendance a WHERE a.checkInTime BETWEEN :start AND :end ORDER BY a.checkInTime DESC")
    List<Attendance> findCheckInsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
