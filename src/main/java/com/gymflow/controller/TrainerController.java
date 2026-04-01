package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.TrainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerController {

    private final TrainerService trainerService;

    @PostMapping
    public ResponseEntity<TrainerResponse> create(@Valid @RequestBody TrainerRequest request) {
        return ResponseEntity.ok(trainerService.createTrainer(request));
    }

    @GetMapping
    public ResponseEntity<List<TrainerResponse>> getAll() {
        return ResponseEntity.ok(trainerService.getAllTrainers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainerResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(trainerService.getTrainer(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainerResponse> update(@PathVariable UUID id, @Valid @RequestBody TrainerRequest request) {
        return ResponseEntity.ok(trainerService.updateTrainer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        trainerService.deleteTrainer(id);
        return ResponseEntity.noContent().build();
    }
}
