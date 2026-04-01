package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ResponseEntity<AttendanceResponse> checkIn(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.checkIn(request));
    }

    @PostMapping("/checkout/{memberId}")
    public ResponseEntity<AttendanceResponse> checkOut(@PathVariable UUID memberId) {
        return ResponseEntity.ok(attendanceService.checkOut(memberId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AttendanceResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getAllAttendance(page, size));
    }

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceResponse>> getToday() {
        return ResponseEntity.ok(attendanceService.getTodayCheckIns());
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<PageResponse<AttendanceResponse>> getMemberAttendance(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getMemberAttendance(memberId, page, size));
    }
}
