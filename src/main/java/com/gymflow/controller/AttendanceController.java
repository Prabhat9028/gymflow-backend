package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/attendance") @RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService svc;
    @PostMapping("/checkin") public ResponseEntity<AttendanceResponse> checkIn(@RequestBody AttendanceRequest req, @RequestParam UUID branchId) { return ResponseEntity.ok(svc.checkIn(req, branchId)); }
    @PostMapping("/checkout/{mid}") public ResponseEntity<AttendanceResponse> checkOut(@PathVariable UUID mid) { return ResponseEntity.ok(svc.checkOut(mid)); }
    @GetMapping("/today") public ResponseEntity<List<AttendanceResponse>> today(@RequestParam UUID branchId) { return ResponseEntity.ok(svc.getToday(branchId)); }
    @GetMapping("/by-date") public ResponseEntity<List<AttendanceResponse>> byDate(@RequestParam UUID branchId, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) { return ResponseEntity.ok(svc.getByDate(branchId, date)); }
    @GetMapping public ResponseEntity<PageResponse<AttendanceResponse>> all(@RequestParam UUID branchId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(svc.getAll(branchId, page, size)); }
}
