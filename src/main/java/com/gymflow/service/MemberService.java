package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BiometricDataRepository biometricDataRepository;

    @Transactional
    public MemberResponse createMember(MemberRequest req) {
        Member member = Member.builder()
                .memberCode(generateMemberCode())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .gender(req.getGender() != null ? Member.Gender.valueOf(req.getGender()) : null)
                .dateOfBirth(req.getDateOfBirth())
                .address(req.getAddress())
                .emergencyContactName(req.getEmergencyContactName())
                .emergencyContactPhone(req.getEmergencyContactPhone())
                .photoUrl(req.getPhotoUrl())
                .joinDate(LocalDate.now())
                .isActive(true)
                .build();

        member = memberRepository.save(member);
        return toResponse(member);
    }

    public MemberResponse getMember(UUID id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return toResponse(m);
    }

    public MemberResponse getMemberByCode(String code) {
        Member m = memberRepository.findByMemberCode(code)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return toResponse(m);
    }

    public PageResponse<MemberResponse> getAllMembers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Member> pg;

        if (search != null && !search.isBlank()) {
            pg = memberRepository.search(search.trim(), pageable);
        } else {
            pg = memberRepository.findAll(pageable);
        }

        List<MemberResponse> content = pg.getContent().stream().map(this::toResponse).toList();
        return PageResponse.<MemberResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .build();
    }

    @Transactional
    public MemberResponse updateMember(UUID id, MemberRequest req) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (req.getFirstName() != null) m.setFirstName(req.getFirstName());
        if (req.getLastName() != null) m.setLastName(req.getLastName());
        if (req.getEmail() != null) m.setEmail(req.getEmail());
        if (req.getPhone() != null) m.setPhone(req.getPhone());
        if (req.getGender() != null) m.setGender(Member.Gender.valueOf(req.getGender()));
        if (req.getDateOfBirth() != null) m.setDateOfBirth(req.getDateOfBirth());
        if (req.getAddress() != null) m.setAddress(req.getAddress());
        if (req.getEmergencyContactName() != null) m.setEmergencyContactName(req.getEmergencyContactName());
        if (req.getEmergencyContactPhone() != null) m.setEmergencyContactPhone(req.getEmergencyContactPhone());
        if (req.getPhotoUrl() != null) m.setPhotoUrl(req.getPhotoUrl());

        return toResponse(memberRepository.save(m));
    }

    @Transactional
    public void deactivateMember(UUID id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        m.setIsActive(false);
        memberRepository.save(m);
    }

    public long countActiveMembers() {
        return memberRepository.countByIsActiveTrue();
    }

    public List<MemberResponse> getRecentMembers(int limit) {
        return memberRepository.findRecentMembers(PageRequest.of(0, limit))
                .stream().map(this::toResponse).toList();
    }

    private MemberResponse toResponse(Member m) {
        SubscriptionResponse activeSub = null;
        try {
            var sub = subscriptionRepository.findActiveMembership(m.getId(), Subscription.MembershipStatus.ACTIVE);
            if (sub.isPresent()) {
                Subscription s = sub.get();
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate());
                activeSub = SubscriptionResponse.builder()
                        .id(s.getId())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .status(s.getStatus().name())
                        .amountPaid(s.getAmountPaid())
                        .daysRemaining(Math.max(0, daysRemaining))
                        .plan(s.getPlan() != null ? PlanResponse.builder()
                                .id(s.getPlan().getId())
                                .name(s.getPlan().getName())
                                .price(s.getPlan().getPrice())
                                .durationDays(s.getPlan().getDurationDays())
                                .build() : null)
                        .build();
            }
        } catch (Exception ignored) {}

        boolean hasBio = biometricDataRepository.existsByMemberIdAndBiometricTypeAndIsActiveTrue(
                m.getId(), BiometricData.BiometricType.FINGERPRINT) ||
                biometricDataRepository.existsByMemberIdAndBiometricTypeAndIsActiveTrue(
                m.getId(), BiometricData.BiometricType.FACE);

        return MemberResponse.builder()
                .id(m.getId())
                .memberCode(m.getMemberCode())
                .firstName(m.getFirstName())
                .lastName(m.getLastName())
                .email(m.getEmail())
                .phone(m.getPhone())
                .gender(m.getGender() != null ? m.getGender().name() : null)
                .dateOfBirth(m.getDateOfBirth())
                .address(m.getAddress())
                .emergencyContactName(m.getEmergencyContactName())
                .emergencyContactPhone(m.getEmergencyContactPhone())
                .photoUrl(m.getPhotoUrl())
                .joinDate(m.getJoinDate())
                .isActive(m.getIsActive())
                .activeSubscription(activeSub)
                .hasBiometric(hasBio)
                .build();
    }

    private String generateMemberCode() {
        String code;
        do {
            code = "GF" + String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999999));
        } while (memberRepository.findByMemberCode(code).isPresent());
        return code;
    }
}
