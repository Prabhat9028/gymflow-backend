package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(memberService.createMember(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MemberResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(memberService.getAllMembers(page, size, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(memberService.getMember(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<MemberResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(memberService.getMemberByCode(code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> update(@PathVariable UUID id, @Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(memberService.updateMember(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        memberService.deactivateMember(id);
        return ResponseEntity.noContent().build();
    }
}
