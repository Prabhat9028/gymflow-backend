package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.BiometricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/biometric")
@RequiredArgsConstructor
public class BiometricController {

    private final BiometricService biometricService;

    @PostMapping("/enroll")
    public ResponseEntity<BiometricResponse> enroll(@Valid @RequestBody BiometricEnrollRequest request) {
        return ResponseEntity.ok(biometricService.enrollBiometric(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<BiometricVerifyResponse> verify(@Valid @RequestBody BiometricVerifyRequest request) {
        return ResponseEntity.ok(biometricService.verifyBiometric(request));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<BiometricResponse>> getMemberBiometrics(@PathVariable UUID memberId) {
        return ResponseEntity.ok(biometricService.getMemberBiometrics(memberId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        biometricService.deleteBiometric(id);
        return ResponseEntity.noContent().build();
    }
}
