package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricService {

    private final BiometricDataRepository biometricRepo;
    private final MemberRepository memberRepository;
    private final AttendanceService attendanceService;
    private final SubscriptionRepository subscriptionRepository;

    private static final BigDecimal MATCH_THRESHOLD = new BigDecimal("75.00");

    @Transactional
    public BiometricResponse enrollBiometric(BiometricEnrollRequest req) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        BiometricData.BiometricType type = BiometricData.BiometricType.valueOf(req.getBiometricType().toUpperCase());

        // Check if already enrolled
        var existing = biometricRepo.findByMemberIdAndBiometricType(member.getId(), type);
        if (existing.isPresent()) {
            // Update existing
            BiometricData bd = existing.get();
            bd.setTemplateData(req.getTemplateData());
            bd.setTemplateHash(hashTemplate(req.getTemplateData()));
            bd.setDeviceId(req.getDeviceId());
            bd.setIsActive(true);
            bd.setEnrolledAt(LocalDateTime.now());
            bd = biometricRepo.save(bd);
            return toResponse(bd, member);
        }

        BiometricData bd = BiometricData.builder()
                .member(member)
                .biometricType(type)
                .templateData(req.getTemplateData())
                .templateHash(hashTemplate(req.getTemplateData()))
                .deviceId(req.getDeviceId())
                .enrolledAt(LocalDateTime.now())
                .isActive(true)
                .build();

        bd = biometricRepo.save(bd);
        return toResponse(bd, member);
    }

    /**
     * Verify a biometric template against all enrolled templates.
     * In production, this would use a biometric SDK (e.g., SecuGen, ZKTeco, Suprema).
     * This implementation simulates matching using template hash comparison
     * and a scoring algorithm for demonstration.
     */
    @Transactional
    public BiometricVerifyResponse verifyBiometric(BiometricVerifyRequest req) {
        BiometricData.BiometricType type = BiometricData.BiometricType.valueOf(req.getBiometricType().toUpperCase());
        String incomingHash = hashTemplate(req.getTemplateData());

        // Strategy 1: Exact hash match (fast path)
        var exactMatch = biometricRepo.findByTemplateHash(incomingHash);
        if (exactMatch.isPresent() && exactMatch.get().getIsActive()) {
            BiometricData bd = exactMatch.get();
            BigDecimal score = new BigDecimal("99.50");
            return processMatch(bd, score, req.getDeviceId());
        }

        // Strategy 2: Template comparison against all active templates
        // In production, this uses biometric SDK's 1:N matching
        List<BiometricData> candidates = biometricRepo.findAllActiveByType(type);

        BiometricData bestMatch = null;
        BigDecimal bestScore = BigDecimal.ZERO;

        for (BiometricData candidate : candidates) {
            BigDecimal score = computeMatchScore(req.getTemplateData(), candidate.getTemplateData());
            if (score.compareTo(bestScore) > 0) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestMatch != null && bestScore.compareTo(MATCH_THRESHOLD) >= 0) {
            return processMatch(bestMatch, bestScore, req.getDeviceId());
        }

        return BiometricVerifyResponse.builder()
                .matched(false)
                .matchScore(bestScore)
                .build();
    }

    public List<BiometricResponse> getMemberBiometrics(UUID memberId) {
        return biometricRepo.findByMemberIdAndIsActiveTrue(memberId).stream()
                .map(bd -> toResponse(bd, bd.getMember()))
                .toList();
    }

    @Transactional
    public void deleteBiometric(UUID biometricId) {
        BiometricData bd = biometricRepo.findById(biometricId)
                .orElseThrow(() -> new RuntimeException("Biometric record not found"));
        bd.setIsActive(false);
        biometricRepo.save(bd);
    }

    private BiometricVerifyResponse processMatch(BiometricData bd, BigDecimal score, String deviceId) {
        bd.setLastVerifiedAt(LocalDateTime.now());
        biometricRepo.save(bd);

        Member member = bd.getMember();

        // Check membership status
        String membershipStatus = "NO_MEMBERSHIP";
        var activeSub = subscriptionRepository.findActiveMembership(member.getId(), Subscription.MembershipStatus.ACTIVE);
        if (activeSub.isPresent()) {
            membershipStatus = activeSub.get().getStatus().name();
        }

        // Auto check-in
        UUID attendanceId = null;
        try {
            AttendanceRequest attReq = new AttendanceRequest();
            attReq.setMemberId(member.getId());
            attReq.setVerificationMethod("BIOMETRIC");
            attReq.setBiometricMatchScore(score);
            attReq.setDeviceId(deviceId);
            AttendanceResponse att = attendanceService.checkIn(attReq);
            attendanceId = att.getId();
        } catch (Exception e) {
            log.warn("Auto check-in failed for member {}: {}", member.getMemberCode(), e.getMessage());
        }

        return BiometricVerifyResponse.builder()
                .matched(true)
                .memberId(member.getId())
                .memberName(member.getFirstName() + " " + member.getLastName())
                .memberCode(member.getMemberCode())
                .matchScore(score)
                .membershipStatus(membershipStatus)
                .attendanceId(attendanceId)
                .build();
    }

    /**
     * Compute match score between two templates.
     * Production: Replace with biometric SDK call (SecuGen, ZKTeco, Suprema API).
     * This simulation uses string similarity for demonstration purposes.
     */
    private BigDecimal computeMatchScore(String template1, String template2) {
        if (template1 == null || template2 == null) return BigDecimal.ZERO;
        if (template1.equals(template2)) return new BigDecimal("99.50");

        // Simplified Jaccard-like similarity on character bigrams
        int maxLen = Math.max(template1.length(), template2.length());
        if (maxLen == 0) return BigDecimal.ZERO;

        int matches = 0;
        int minLen = Math.min(template1.length(), template2.length());
        for (int i = 0; i < minLen; i++) {
            if (template1.charAt(i) == template2.charAt(i)) matches++;
        }

        double similarity = (double) matches / maxLen * 100.0;
        return BigDecimal.valueOf(similarity).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String hashTemplate(String template) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(template.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private BiometricResponse toResponse(BiometricData bd, Member member) {
        return BiometricResponse.builder()
                .id(bd.getId())
                .memberId(member.getId())
                .memberName(member.getFirstName() + " " + member.getLastName())
                .biometricType(bd.getBiometricType().name())
                .isActive(bd.getIsActive())
                .enrolledAt(bd.getEnrolledAt())
                .lastVerifiedAt(bd.getLastVerifiedAt())
                .build();
    }
}
