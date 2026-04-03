package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/login") public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) { return ResponseEntity.ok(authService.login(req)); }
    @PostMapping("/switch-branch") public ResponseEntity<AuthResponse> switchBranch(Authentication auth, @RequestBody Map<String,String> body) {
        return ResponseEntity.ok(authService.switchBranch(auth.getName(), UUID.fromString(body.get("branchId")))); }
}
