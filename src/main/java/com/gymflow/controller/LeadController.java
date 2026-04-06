package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/leads") @RequiredArgsConstructor
public class LeadController {
    private final LeadService svc;

    @PostMapping public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest req, @RequestParam UUID branchId) { return ResponseEntity.ok(svc.create(req, branchId)); }
    @GetMapping public ResponseEntity<PageResponse<LeadResponse>> getAll(@RequestParam UUID branchId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search) { return ResponseEntity.ok(svc.getAll(branchId, page, size, search)); }
    @GetMapping("/{id}") public ResponseEntity<LeadResponse> get(@PathVariable UUID id) { return ResponseEntity.ok(svc.get(id)); }
    @PutMapping("/{id}") public ResponseEntity<LeadResponse> update(@PathVariable UUID id, @RequestBody LeadRequest req) { return ResponseEntity.ok(svc.update(id, req)); }
    @PutMapping("/{id}/status") public ResponseEntity<LeadResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody LeadStatusUpdate req) { return ResponseEntity.ok(svc.updateStatus(id, req)); }
    @PostMapping("/{id}/convert") public ResponseEntity<LeadResponse> convert(@PathVariable UUID id) { return ResponseEntity.ok(svc.convertToMember(id)); }
    @GetMapping("/{id}/activities") public ResponseEntity<List<LeadActivityResponse>> activities(@PathVariable UUID id) { return ResponseEntity.ok(svc.getActivities(id)); }
    @PostMapping("/{id}/activities") public ResponseEntity<LeadActivityResponse> addActivity(@PathVariable UUID id, @RequestBody LeadStatusUpdate req) { return ResponseEntity.ok(svc.addActivity(id, req)); }
    @GetMapping("/by-status") public ResponseEntity<List<LeadResponse>> byStatus(@RequestParam UUID branchId, @RequestParam String status) { return ResponseEntity.ok(svc.getByStatus(branchId, status)); }
    @GetMapping("/dashboard") public ResponseEntity<LeadDashboard> dashboard(@RequestParam UUID branchId) { return ResponseEntity.ok(svc.getDashboard(branchId)); }
}
