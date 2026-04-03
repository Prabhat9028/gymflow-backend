package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor
public class DashboardController {
    private final DashboardService svc;
    @GetMapping public ResponseEntity<DashboardStats> get(@RequestParam UUID branchId) { return ResponseEntity.ok(svc.getStats(branchId)); }
}
