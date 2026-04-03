package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/staff") @RequiredArgsConstructor
public class StaffController {
    private final StaffService svc;
    @PostMapping public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest req, @RequestParam UUID branchId, @RequestParam UUID companyId) { return ResponseEntity.ok(svc.create(req, branchId, companyId)); }
    @GetMapping public ResponseEntity<PageResponse<StaffResponse>> all(@RequestParam UUID branchId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search) { return ResponseEntity.ok(svc.getAll(branchId, page, size, search)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) { svc.deactivate(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/attendance/checkin") public ResponseEntity<StaffAttendanceResponse> checkIn(@RequestBody StaffAttendanceRequest req, @RequestParam UUID branchId) { return ResponseEntity.ok(svc.staffCheckIn(req, branchId)); }
    @PostMapping("/attendance/checkout/{sid}") public ResponseEntity<StaffAttendanceResponse> checkOut(@PathVariable UUID sid) { return ResponseEntity.ok(svc.staffCheckOut(sid)); }
    @GetMapping("/attendance/today") public ResponseEntity<List<StaffAttendanceResponse>> todayAtt(@RequestParam UUID branchId) { return ResponseEntity.ok(svc.getTodayStaffAtt(branchId)); }
}
