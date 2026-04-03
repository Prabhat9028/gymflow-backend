package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service @RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepo;
    private final SubscriptionRepository subRepo;
    private final BranchRepository branchRepo;
    private final CompanyRepository companyRepo;

    @Transactional
    public MemberResponse create(MemberRequest req, UUID branchId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        Member m = Member.builder().memberCode(genCode()).firstName(req.getFirstName()).lastName(req.getLastName())
            .email(req.getEmail()).phone(req.getPhone())
            .gender(req.getGender() != null ? Member.Gender.valueOf(req.getGender()) : null)
            .dateOfBirth(req.getDateOfBirth()).address(req.getAddress())
            .emergencyContactName(req.getEmergencyContactName()).emergencyContactPhone(req.getEmergencyContactPhone())
            .company(branch.getCompany()).branch(branch).joinDate(LocalDate.now()).isActive(true).biometricEnrolled(false).build();
        return toResponse(memberRepo.save(m));
    }
    public MemberResponse get(UUID id) { return toResponse(memberRepo.findById(id).orElseThrow(() -> new RuntimeException("Member not found"))); }
    public PageResponse<MemberResponse> getAll(UUID branchId, int page, int size, String search) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Member> pg = (search != null && !search.isBlank()) ? memberRepo.searchByBranch(branchId, search.trim(), p) : memberRepo.findByBranchIdAndIsActiveTrue(branchId, p);
        return PageResponse.<MemberResponse>builder().content(pg.getContent().stream().map(this::toResponse).toList())
            .page(pg.getNumber()).size(pg.getSize()).totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages()).build();
    }
    @Transactional
    public MemberResponse update(UUID id, MemberRequest req) {
        Member m = memberRepo.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        if (req.getFirstName() != null) m.setFirstName(req.getFirstName());
        if (req.getLastName() != null) m.setLastName(req.getLastName());
        if (req.getEmail() != null) m.setEmail(req.getEmail());
        if (req.getPhone() != null) m.setPhone(req.getPhone());
        if (req.getAddress() != null) m.setAddress(req.getAddress());
        if (req.getGender() != null) m.setGender(Member.Gender.valueOf(req.getGender()));
        if (req.getDateOfBirth() != null) m.setDateOfBirth(req.getDateOfBirth());
        if (req.getEmergencyContactName() != null) m.setEmergencyContactName(req.getEmergencyContactName());
        if (req.getEmergencyContactPhone() != null) m.setEmergencyContactPhone(req.getEmergencyContactPhone());
        return toResponse(memberRepo.save(m));
    }
    public void deactivate(UUID id) { Member m = memberRepo.findById(id).orElseThrow(); m.setIsActive(false); memberRepo.save(m); }
    public long countActive(UUID branchId) { return memberRepo.countByBranchIdAndIsActiveTrue(branchId); }
    public List<MemberResponse> getRecent(UUID branchId, int limit) {
        return memberRepo.findRecentByBranch(branchId, PageRequest.of(0, limit)).stream().map(this::toResponse).toList();
    }

    private MemberResponse toResponse(Member m) {
        SubscriptionResponse activeSub = null;
        try {
            var s = subRepo.findActiveMembership(m.getId(), Subscription.MembershipStatus.ACTIVE);
            if (s.isPresent()) {
                Subscription sub = s.get();
                activeSub = SubscriptionResponse.builder().id(sub.getId()).startDate(sub.getStartDate()).endDate(sub.getEndDate())
                    .status(sub.getStatus().name()).amountPaid(sub.getAmountPaid())
                    .daysRemaining(Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate())))
                    .plan(sub.getPlan() != null ? PlanResponse.builder().id(sub.getPlan().getId()).name(sub.getPlan().getName())
                        .price(sub.getPlan().getPrice()).durationDays(sub.getPlan().getDurationDays()).build() : null).build();
            }
        } catch (Exception ignored) {}
        return MemberResponse.builder().id(m.getId()).memberCode(m.getMemberCode()).firstName(m.getFirstName()).lastName(m.getLastName())
            .email(m.getEmail()).phone(m.getPhone()).gender(m.getGender() != null ? m.getGender().name() : null)
            .dateOfBirth(m.getDateOfBirth()).address(m.getAddress()).emergencyContactName(m.getEmergencyContactName())
            .emergencyContactPhone(m.getEmergencyContactPhone()).joinDate(m.getJoinDate()).isActive(m.getIsActive())
            .deviceUserId(m.getDeviceUserId()).biometricEnrolled(m.getBiometricEnrolled())
            .activeSubscription(activeSub).branchId(m.getBranch() != null ? m.getBranch().getId() : null)
            .branchName(m.getBranch() != null ? m.getBranch().getName() : null).build();
    }
    private String genCode() { String c; do { c = "GF" + String.format("%06d", ThreadLocalRandom.current().nextInt(1,999999)); } while (memberRepo.findByMemberCode(c).isPresent()); return c; }
}
