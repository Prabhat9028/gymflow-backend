package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.TrainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/trainers") @RequiredArgsConstructor
public class TrainerController {
    private final TrainerService svc;
    @PostMapping public ResponseEntity<TrainerResponse> create(@Valid @RequestBody TrainerRequest req, @RequestParam UUID branchId, @RequestParam UUID companyId) { return ResponseEntity.ok(svc.create(req, branchId, companyId)); }
    @GetMapping public ResponseEntity<List<TrainerResponse>> all(@RequestParam UUID branchId) { return ResponseEntity.ok(svc.getAll(branchId)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) { svc.delete(id); return ResponseEntity.noContent().build(); }
}
