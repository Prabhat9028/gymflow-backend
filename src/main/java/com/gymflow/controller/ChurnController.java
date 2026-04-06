package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.ChurnPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/churn")
@RequiredArgsConstructor
public class ChurnController {

    private final ChurnPredictionService churnService;

    @GetMapping
    public ResponseEntity<ChurnDashboard> getChurnPredictions(@RequestParam UUID branchId) {
        return ResponseEntity.ok(churnService.predictChurn(branchId));
    }
}
