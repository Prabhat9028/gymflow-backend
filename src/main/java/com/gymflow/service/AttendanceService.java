package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attRepo;
    private final MemberRepository memberRepo;
    private final SubscriptionRepository subRepo;

    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest req, UUID branchId) {
        UUID mid = req.getMemberId();
        if (mid == null && req.getMemberCode() != null)
            mid = memberRepo.findByMemberCode(req.getMemberCode()).orElseThrow(() -> new RuntimeException("Member not found")).getId();
        Member m = memberRepo.findById(mid).orElseThrow(() -> new RuntimeException("Member not found"));
        attRepo.findOpenCheckIn(m.getId()).ifPresent(o -> { o.setCheckOutTime(LocalDateTime.now()); attRepo.save(o); });
        Branch branch = m.getBranch();
        Attendance a = Attendance.builder().member(m).branch(branch).checkInTime(LocalDateTime.now())
            .verificationMethod(req.getVerificationMethod() != null ? req.getVerificationMethod() : "MANUAL")
            .deviceId(req.getDeviceId()).build();
        return toResponse(attRepo.save(a));
    }
    @Transactional
    public AttendanceResponse checkOut(UUID memberId) {
        var o = attRepo.findOpenCheckIn(memberId).orElseThrow(() -> new RuntimeException("No active check-in"));
        o.setCheckOutTime(LocalDateTime.now()); return toResponse(attRepo.save(o));
    }
    public List<AttendanceResponse> getToday(UUID branchId) {
        LocalDateTime s = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return attRepo.findByBranchBetween(branchId, s, s.plusDays(1)).stream().map(this::toResponse).toList();
    }
    public long countToday(UUID branchId) {
        LocalDateTime s = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return attRepo.countByBranchBetween(branchId, s, s.plusDays(1));
    }
    public long countBetween(UUID branchId, LocalDateTime start, LocalDateTime end) { return attRepo.countByBranchBetween(branchId, start, end); }
    public List<AttendanceResponse> getByDate(UUID branchId, LocalDate date) {
        LocalDateTime s = date.atStartOfDay(), e = date.plusDays(1).atStartOfDay();
        return attRepo.findByBranchBetween(branchId, s, e).stream().map(this::toResponse).toList();
    }
    public List<AttendanceResponse> getRecent(UUID branchId, int limit) {
        return attRepo.findRecentByBranch(branchId, PageRequest.of(0, limit)).stream().map(this::toResponse).toList();
    }
    public PageResponse<AttendanceResponse> getAll(UUID branchId, int page, int size) {
        Page<Attendance> pg = attRepo.findByBranchIdOrderByCheckInTimeDesc(branchId, PageRequest.of(page, size));
        return PageResponse.<AttendanceResponse>builder().content(pg.getContent().stream().map(this::toResponse).toList())
            .page(pg.getNumber()).size(pg.getSize()).totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages()).build();
    }
    private AttendanceResponse toResponse(Attendance a) {
        String dur = null;
        if (a.getCheckOutTime() != null) { Duration d = Duration.between(a.getCheckInTime(), a.getCheckOutTime()); dur = d.toHours() + "h " + d.toMinutesPart() + "m"; }
        // Get subscription info
        LocalDate subEnd = null; String subStatus = null;
        try {
            var activeSub = subRepo.findActiveSub(a.getMember().getId(), Subscription.MembershipStatus.ACTIVE, Subscription.SubType.MEMBERSHIP);
            if (activeSub.isPresent()) { subEnd = activeSub.get().getEndDate(); subStatus = "ACTIVE"; }
            else { subStatus = "EXPIRED"; }
        } catch (Exception ignored) {}
        return AttendanceResponse.builder().id(a.getId()).memberId(a.getMember().getId())
            .memberName(a.getMember().getFirstName() + " " + a.getMember().getLastName())
            .memberCode(a.getMember().getMemberCode()).memberPhone(a.getMember().getPhone())
            .subscriptionEndDate(subEnd).subscriptionStatus(subStatus)
            .checkInTime(a.getCheckInTime())
            .checkOutTime(a.getCheckOutTime()).verificationMethod(a.getVerificationMethod()).duration(dur).build();
    }
}
