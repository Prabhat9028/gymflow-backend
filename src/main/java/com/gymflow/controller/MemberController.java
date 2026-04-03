package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.biometric.EsslDeviceService;
import com.gymflow.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/members") @RequiredArgsConstructor
public class MemberController {
    private final MemberService svc;
    private final EsslDeviceService esslService;

    @PostMapping public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest req, @RequestParam UUID branchId) { return ResponseEntity.ok(svc.create(req, branchId)); }
    @GetMapping public ResponseEntity<PageResponse<MemberResponse>> getAll(@RequestParam UUID branchId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String search) { return ResponseEntity.ok(svc.getAll(branchId, page, size, search)); }
    @GetMapping("/{id}") public ResponseEntity<MemberResponse> get(@PathVariable UUID id) { return ResponseEntity.ok(svc.get(id)); }
    @PutMapping("/{id}") public ResponseEntity<MemberResponse> update(@PathVariable UUID id, @Valid @RequestBody MemberRequest req) { return ResponseEntity.ok(svc.update(id, req)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) { svc.deactivate(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/{id}/enroll-biometric")
    public ResponseEntity<Map<String,Object>> enrollBiometric(@PathVariable UUID id, @RequestParam String deviceSerial) {
        return ResponseEntity.ok(esslService.enrollFingerprint(id, deviceSerial));
    }
}
