package com.gymflow.controller;
import com.gymflow.biometric.EsslDeviceService;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/biometric") @RequiredArgsConstructor
public class BiometricController {
    private final EsslDeviceService esslService;
    private final BiometricDeviceRepository deviceRepo;
    private final BranchRepository branchRepo;
    private final CompanyRepository companyRepo;

    @PostMapping("/enroll") public ResponseEntity<Map<String,Object>> enroll(@RequestBody Map<String,String> body) {
        return ResponseEntity.ok(esslService.enrollFingerprint(UUID.fromString(body.get("memberId")), body.get("deviceSerial")));
    }
    @PostMapping("/pull-attendance") public ResponseEntity<Map<String,Object>> pull(@RequestParam String deviceSerial) {
        return ResponseEntity.ok(esslService.pullAttendanceLogs(deviceSerial));
    }
    @GetMapping("/devices") public ResponseEntity<List<DeviceResponse>> devices(@RequestParam UUID branchId) {
        return ResponseEntity.ok(deviceRepo.findByBranchIdAndIsActiveTrue(branchId).stream().map(this::toDevResp).toList());
    }
    @PostMapping("/devices") public ResponseEntity<DeviceResponse> addDevice(@Valid @RequestBody DeviceRequest req, @RequestParam UUID branchId, @RequestParam UUID companyId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        Company company = companyRepo.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));
        if (deviceRepo.findByDeviceSerial(req.getDeviceSerial()).isPresent()) throw new RuntimeException("Device serial already exists");
        BiometricDevice d = BiometricDevice.builder()
            .company(company).branch(branch)
            .deviceSerial(req.getDeviceSerial()).deviceName(req.getDeviceName())
            .deviceIp(req.getDeviceIp()).devicePort(req.getDevicePort() != null ? req.getDevicePort() : 4370)
            .deviceType(req.getDeviceType() != null ? req.getDeviceType() : "ESSL_ZK")
            .isActive(true).build();
        return ResponseEntity.ok(toDevResp(deviceRepo.save(d)));
    }
    @DeleteMapping("/devices/{id}") public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        BiometricDevice d = deviceRepo.findById(id).orElseThrow(); d.setIsActive(false); deviceRepo.save(d); return ResponseEntity.noContent().build();
    }
    private DeviceResponse toDevResp(BiometricDevice d) {
        return DeviceResponse.builder().id(d.getId()).deviceSerial(d.getDeviceSerial()).deviceName(d.getDeviceName())
            .deviceIp(d.getDeviceIp()).devicePort(d.getDevicePort()).deviceType(d.getDeviceType())
            .lastHeartbeat(d.getLastHeartbeat()).isActive(d.getIsActive())
            .branchId(d.getBranch().getId()).branchName(d.getBranch().getName()).build();
    }
}
