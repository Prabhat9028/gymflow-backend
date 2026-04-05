package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service @RequiredArgsConstructor
public class StaffService {
    private final StaffRepository staffRepo;
    private final StaffAttendanceRepository saRepo;
    private final UserRepository userRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder encoder;

    @Transactional
    public StaffResponse create(StaffRequest req, UUID branchId, UUID companyId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow();
        User user = null;
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            if (userRepo.existsByEmail(req.getEmail())) throw new RuntimeException("Email already registered");
            String pw = (req.getPassword() != null && !req.getPassword().isBlank()) ? req.getPassword() : "gymflow123";
            String role = req.getRole() != null ? req.getRole() : "STAFF";
            User.UserRole ur = switch (role.toUpperCase()) { case "ADMIN","MANAGER" -> User.UserRole.ADMIN; case "TRAINER" -> User.UserRole.TRAINER; default -> User.UserRole.STAFF; };
            user = userRepo.save(User.builder().email(req.getEmail()).passwordHash(encoder.encode(pw)).role(ur).company(branch.getCompany()).branch(branch).isActive(true).build());
        }
        Staff s = Staff.builder().user(user).company(branch.getCompany()).branch(branch).staffCode(genCode())
            .firstName(req.getFirstName()).lastName(req.getLastName()).email(req.getEmail()).phone(req.getPhone())
            .role(req.getRole() != null ? req.getRole() : "STAFF").department(req.getDepartment()).designation(req.getDesignation())
            .dateOfBirth(req.getDateOfBirth()).address(req.getAddress()).salary(req.getSalary())
            .joinDate(LocalDate.now()).shiftStart(req.getShiftStart()).shiftEnd(req.getShiftEnd()).isActive(true).build();
        return toR(staffRepo.save(s));
    }
    public PageResponse<StaffResponse> getAll(UUID branchId, int page, int size, String search) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Staff> pg = (search != null && !search.isBlank()) ? staffRepo.searchByBranch(branchId, search.trim(), p) : staffRepo.findByBranchId(branchId, p);
        return PageResponse.<StaffResponse>builder().content(pg.getContent().stream().map(this::toR).toList())
            .page(pg.getNumber()).size(pg.getSize()).totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages()).build();
    }
    public void deactivate(UUID id) { Staff s = staffRepo.findById(id).orElseThrow(); s.setIsActive(false); staffRepo.save(s); if (s.getUser() != null) { s.getUser().setIsActive(false); userRepo.save(s.getUser()); } }

    @Transactional
    public StaffAttendanceResponse staffCheckIn(StaffAttendanceRequest req, UUID branchId) {
        UUID sid = req.getStaffId();
        if (sid == null && req.getStaffCode() != null) sid = staffRepo.findByStaffCode(req.getStaffCode()).orElseThrow(() -> new RuntimeException("Staff not found")).getId();
        Staff staff = staffRepo.findById(sid).orElseThrow();
        saRepo.findOpenCheckIn(staff.getId()).ifPresent(o -> { o.setCheckOutTime(LocalDateTime.now()); saRepo.save(o); });
        int late = 0;
        if (staff.getShiftStart() != null && LocalTime.now().isAfter(staff.getShiftStart()))
            late = (int) Duration.between(staff.getShiftStart(), LocalTime.now()).toMinutes();
        StaffAttendance sa = StaffAttendance.builder().staff(staff).branch(staff.getBranch()).checkInTime(LocalDateTime.now())
            .shiftStart(staff.getShiftStart()).shiftEnd(staff.getShiftEnd()).status(late > 15 ? "LATE" : "PRESENT").lateMinutes(Math.max(0, late)).notes(req.getNotes()).build();
        return toSA(saRepo.save(sa));
    }
    @Transactional
    public StaffAttendanceResponse staffCheckOut(UUID staffId) {
        StaffAttendance sa = saRepo.findOpenCheckIn(staffId).orElseThrow(() -> new RuntimeException("No active check-in"));
        sa.setCheckOutTime(LocalDateTime.now()); return toSA(saRepo.save(sa));
    }
    public List<StaffAttendanceResponse> getTodayStaffAtt(UUID branchId) {
        LocalDateTime s = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return saRepo.findByBranchBetween(branchId, s, s.plusDays(1)).stream().map(this::toSA).toList();
    }

    private StaffResponse toR(Staff s) { return StaffResponse.builder().id(s.getId()).staffCode(s.getStaffCode()).firstName(s.getFirstName()).lastName(s.getLastName()).email(s.getEmail()).phone(s.getPhone()).role(s.getRole()).department(s.getDepartment()).designation(s.getDesignation()).salary(s.getSalary()).joinDate(s.getJoinDate()).shiftStart(s.getShiftStart()).shiftEnd(s.getShiftEnd()).isActive(s.getIsActive()).branchId(s.getBranch() != null ? s.getBranch().getId() : null).branchName(s.getBranch() != null ? s.getBranch().getName() : null).photoUrl(s.getPhotoUrl()).build(); }
    private StaffAttendanceResponse toSA(StaffAttendance a) {
        String dur = null; if (a.getCheckOutTime() != null) { Duration d = Duration.between(a.getCheckInTime(), a.getCheckOutTime()); dur = d.toHours()+"h "+d.toMinutesPart()+"m"; }
        return StaffAttendanceResponse.builder().id(a.getId()).staffId(a.getStaff().getId()).staffName(a.getStaff().getFirstName()+" "+a.getStaff().getLastName()).staffCode(a.getStaff().getStaffCode()).checkInTime(a.getCheckInTime()).checkOutTime(a.getCheckOutTime()).shiftStart(a.getShiftStart()).shiftEnd(a.getShiftEnd()).status(a.getStatus()).lateMinutes(a.getLateMinutes()).overtimeMinutes(a.getOvertimeMinutes()).duration(dur).build();
    }
    private String genCode() { String c; do { c = "STF" + String.format("%04d", ThreadLocalRandom.current().nextInt(1,9999)); } while (staffRepo.findByStaffCode(c).isPresent()); return c; }
}
