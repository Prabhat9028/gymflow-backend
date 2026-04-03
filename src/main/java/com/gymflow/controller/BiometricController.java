package com.gymflow.controller;
import com.gymflow.biometric.EsslDeviceService;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.BiometricDevice;
import com.gymflow.repository.BiometricDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/biometric") @RequiredArgsConstructor
public class BiometricController {
    private final EsslDeviceService esslService;
    private final BiometricDeviceRepository deviceRepo;

    @PostMapping("/enroll") public ResponseEntity<Map<String,Object>> enroll(@RequestBody Map<String,String> body) {
        return ResponseEntity.ok(esslService.enrollFingerprint(UUID.fromString(body.get("memberId")), body.get("deviceSerial")));
    }
    @PostMapping("/pull-attendance") public ResponseEntity<Map<String,Object>> pull(@RequestParam String deviceSerial) {
        return ResponseEntity.ok(esslService.pullAttendanceLogs(deviceSerial));
    }
    @GetMapping("/devices") public ResponseEntity<List<DeviceResponse>> devices(@RequestParam UUID branchId) {
        return ResponseEntity.ok(deviceRepo.findByBranchIdAndIsActiveTrue(branchId).stream().map(d ->
            DeviceResponse.builder().id(d.getId()).deviceSerial(d.getDeviceSerial()).deviceName(d.getDeviceName())
                .deviceIp(d.getDeviceIp()).devicePort(d.getDevicePort()).deviceType(d.getDeviceType())
                .lastHeartbeat(d.getLastHeartbeat()).isActive(d.getIsActive())
                .branchId(d.getBranch().getId()).branchName(d.getBranch().getName()).build()).toList());
    }
}
