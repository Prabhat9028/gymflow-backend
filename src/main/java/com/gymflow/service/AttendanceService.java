package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest req) {
        UUID memberId = req.getMemberId();
        if (memberId == null && req.getMemberCode() != null) {
            Member m = memberRepository.findByMemberCode(req.getMemberCode())
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            memberId = m.getId();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check for open check-in (auto checkout if exists)
        var openCheckIn = attendanceRepository.findOpenCheckIn(member.getId());
        if (openCheckIn.isPresent()) {
            Attendance open = openCheckIn.get();
            open.setCheckOutTime(LocalDateTime.now());
            attendanceRepository.save(open);
        }

        Attendance attendance = Attendance.builder()
                .member(member)
                .checkInTime(LocalDateTime.now())
                .verificationMethod(req.getVerificationMethod() != null ? req.getVerificationMethod() : "MANUAL")
                .biometricMatchScore(req.getBiometricMatchScore())
                .deviceId(req.getDeviceId())
                .build();

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse checkOut(UUID memberId) {
        var open = attendanceRepository.findOpenCheckIn(memberId)
                .orElseThrow(() -> new RuntimeException("No active check-in found"));

        open.setCheckOutTime(LocalDateTime.now());
        return toResponse(attendanceRepository.save(open));
    }

    public PageResponse<AttendanceResponse> getAllAttendance(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Attendance> pg = attendanceRepository.findAllByOrderByCheckInTimeDesc(pageable);
        List<AttendanceResponse> content = pg.getContent().stream().map(this::toResponse).toList();
        return PageResponse.<AttendanceResponse>builder()
                .content(content).page(pg.getNumber()).size(pg.getSize())
                .totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages())
                .build();
    }

    public List<AttendanceResponse> getTodayCheckIns() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return attendanceRepository.findCheckInsBetween(startOfDay, endOfDay).stream().map(this::toResponse).toList();
    }

    public long countTodayCheckIns() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return attendanceRepository.countCheckInsBetween(startOfDay, endOfDay);
    }

    public long countCheckInsBetween(LocalDateTime start, LocalDateTime end) {
        return attendanceRepository.countCheckInsBetween(start, end);
    }

    public List<AttendanceResponse> getRecentCheckIns(int limit) {
        return attendanceRepository.findRecentCheckIns(PageRequest.of(0, limit))
                .stream().map(this::toResponse).toList();
    }

    public PageResponse<AttendanceResponse> getMemberAttendance(UUID memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Attendance> pg = attendanceRepository.findByMemberIdOrderByCheckInTimeDesc(memberId, pageable);
        List<AttendanceResponse> content = pg.getContent().stream().map(this::toResponse).toList();
        return PageResponse.<AttendanceResponse>builder()
                .content(content).page(pg.getNumber()).size(pg.getSize())
                .totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages())
                .build();
    }

    private AttendanceResponse toResponse(Attendance a) {
        String duration = null;
        if (a.getCheckOutTime() != null) {
            Duration d = Duration.between(a.getCheckInTime(), a.getCheckOutTime());
            long hours = d.toHours();
            long mins = d.toMinutesPart();
            duration = hours + "h " + mins + "m";
        }

        return AttendanceResponse.builder()
                .id(a.getId())
                .memberId(a.getMember().getId())
                .memberName(a.getMember().getFirstName() + " " + a.getMember().getLastName())
                .memberCode(a.getMember().getMemberCode())
                .checkInTime(a.getCheckInTime())
                .checkOutTime(a.getCheckOutTime())
                .verificationMethod(a.getVerificationMethod())
                .biometricMatchScore(a.getBiometricMatchScore())
                .duration(duration)
                .build();
    }
}
