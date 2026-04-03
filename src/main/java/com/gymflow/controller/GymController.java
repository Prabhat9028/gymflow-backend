package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.GymManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gyms")
@RequiredArgsConstructor
public class GymController {

    private final GymManagementService gymService;

    // ===== GYM (COMPANY) =====

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CompanyResponse>> getAllGyms() {
        return ResponseEntity.ok(gymService.getAllGyms());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> getGym(@PathVariable UUID id) {
        return ResponseEntity.ok(gymService.getGym(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> createGym(@Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(gymService.createGym(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> updateGym(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(gymService.updateGym(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateGym(@PathVariable UUID id) {
        gymService.deactivateGym(id);
        return ResponseEntity.noContent().build();
    }

    // ===== BRANCHES =====

    @GetMapping("/{companyId}/branches")
    public ResponseEntity<List<BranchResponse>> getBranches(@PathVariable UUID companyId) {
        return ResponseEntity.ok(gymService.getBranches(companyId));
    }

    @GetMapping("/branches/{branchId}")
    public ResponseEntity<BranchResponse> getBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(gymService.getBranch(branchId));
    }

    @PostMapping("/{companyId}/branches")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BranchResponse> createBranch(@PathVariable UUID companyId, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(gymService.createBranch(companyId, request));
    }

    @PutMapping("/branches/{branchId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BranchResponse> updateBranch(@PathVariable UUID branchId, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(gymService.updateBranch(branchId, request));
    }

    @DeleteMapping("/branches/{branchId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateBranch(@PathVariable UUID branchId) {
        gymService.deactivateBranch(branchId);
        return ResponseEntity.noContent().build();
    }
}
