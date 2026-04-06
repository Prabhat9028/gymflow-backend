package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class LeadService {

    private final LeadRepository leadRepo;
    private final LeadActivityRepository actRepo;
    private final BranchRepository branchRepo;
    private final MemberRepository memberRepo;
    private final StaffRepository staffRepo;

    @Transactional
    public LeadResponse create(LeadRequest req, UUID branchId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        // Check duplicate phone
        leadRepo.findByPhoneAndBranchId(req.getPhone(), branchId).ifPresent(e -> {
            throw new RuntimeException("Lead with phone " + req.getPhone() + " already exists: " + e.getFirstName());
        });
        // Also check if already a member
        memberRepo.findByPhoneAndBranchId(req.getPhone(), branchId).ifPresent(m -> {
            throw new RuntimeException("Already a member: " + m.getFirstName() + " " + m.getLastName() + " (" + m.getMemberCode() + ")");
        });

        Staff assignedStaff = null;
        if (req.getAssignedStaffId() != null) assignedStaff = staffRepo.findById(req.getAssignedStaffId()).orElse(null);

        Lead lead = Lead.builder()
            .company(branch.getCompany()).branch(branch)
            .firstName(req.getFirstName()).lastName(req.getLastName())
            .phone(req.getPhone()).email(req.getEmail())
            .gender(req.getGender() != null ? Lead.Gender.valueOf(req.getGender()) : null)
            .status(Lead.LeadStatus.NEW)
            .leadSource(req.getLeadSource()).campaignName(req.getCampaignName()).referredBy(req.getReferredBy())
            .assignedTo(req.getAssignedTo()).assignedStaff(assignedStaff)
            .interestedPlan(req.getInterestedPlan()).expectedJoinDate(req.getExpectedJoinDate())
            .nextFollowUp(req.getNextFollowUp()).notes(req.getNotes())
            .followUpCount(0).build();
        lead = leadRepo.save(lead);

        // Log creation activity
        actRepo.save(LeadActivity.builder().lead(lead).type(LeadActivity.ActivityType.NOTE)
            .notes("Lead created").performedBy(req.getAssignedTo()).newStatus("NEW").build());

        return toResponse(lead);
    }

    public LeadResponse get(UUID id) { return toResponse(leadRepo.findById(id).orElseThrow(() -> new RuntimeException("Lead not found"))); }

    public PageResponse<LeadResponse> getAll(UUID branchId, int page, int size, String search) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Lead> pg = (search != null && !search.isBlank()) ? leadRepo.searchByBranch(branchId, search.trim(), p) : leadRepo.findByBranchIdOrderByCreatedAtDesc(branchId, p);
        return PageResponse.<LeadResponse>builder()
            .content(pg.getContent().stream().map(this::toResponse).toList())
            .page(pg.getNumber()).size(pg.getSize()).totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages()).build();
    }

    public List<LeadResponse> getByStatus(UUID branchId, String status) {
        return leadRepo.findByBranchIdAndStatus(branchId, Lead.LeadStatus.valueOf(status)).stream().map(this::toResponse).toList();
    }

    @Transactional
    public LeadResponse updateStatus(UUID leadId, LeadStatusUpdate update) {
        Lead lead = leadRepo.findById(leadId).orElseThrow(() -> new RuntimeException("Lead not found"));
        String oldStatus = lead.getStatus().name();
        String newStatus = update.getStatus();

        lead.setStatus(Lead.LeadStatus.valueOf(newStatus));
        lead.setLastContacted(LocalDateTime.now());
        if (update.getNextFollowUp() != null) lead.setNextFollowUp(update.getNextFollowUp());
        if (update.getLostReason() != null) lead.setLostReason(update.getLostReason());
        if (update.getNotes() != null && !update.getNotes().isBlank()) lead.setNotes(update.getNotes());
        lead.setFollowUpCount(lead.getFollowUpCount() + 1);

        // Determine activity type
        LeadActivity.ActivityType actType = LeadActivity.ActivityType.STATUS_CHANGE;
        if (update.getCallResponse() != null) actType = LeadActivity.ActivityType.CALL;

        actRepo.save(LeadActivity.builder().lead(lead).type(actType)
            .notes(update.getNotes()).performedBy(update.getPerformedBy())
            .oldStatus(oldStatus).newStatus(newStatus)
            .callResponse(update.getCallResponse())
            .nextFollowUp(update.getNextFollowUp()).build());

        return toResponse(leadRepo.save(lead));
    }

    @Transactional
    public LeadResponse update(UUID id, LeadRequest req) {
        Lead lead = leadRepo.findById(id).orElseThrow(() -> new RuntimeException("Lead not found"));
        if (req.getFirstName() != null) lead.setFirstName(req.getFirstName());
        if (req.getLastName() != null) lead.setLastName(req.getLastName());
        if (req.getPhone() != null) lead.setPhone(req.getPhone());
        if (req.getEmail() != null) lead.setEmail(req.getEmail());
        if (req.getLeadSource() != null) lead.setLeadSource(req.getLeadSource());
        if (req.getAssignedTo() != null) lead.setAssignedTo(req.getAssignedTo());
        if (req.getInterestedPlan() != null) lead.setInterestedPlan(req.getInterestedPlan());
        if (req.getExpectedJoinDate() != null) lead.setExpectedJoinDate(req.getExpectedJoinDate());
        if (req.getNextFollowUp() != null) lead.setNextFollowUp(req.getNextFollowUp());
        if (req.getNotes() != null) lead.setNotes(req.getNotes());
        if (req.getReferredBy() != null) lead.setReferredBy(req.getReferredBy());
        if (req.getCampaignName() != null) lead.setCampaignName(req.getCampaignName());
        return toResponse(leadRepo.save(lead));
    }

    @Transactional
    public LeadResponse convertToMember(UUID leadId) {
        Lead lead = leadRepo.findById(leadId).orElseThrow(() -> new RuntimeException("Lead not found"));
        if (lead.getStatus() == Lead.LeadStatus.CONVERTED) throw new RuntimeException("Already converted");

        // Check if phone already a member
        memberRepo.findByPhoneAndBranchId(lead.getPhone(), lead.getBranch().getId()).ifPresent(m -> {
            throw new RuntimeException("Phone already registered as member: " + m.getMemberCode());
        });

        // Create member from lead data
        Member m = Member.builder()
            .memberCode(genCode()).firstName(lead.getFirstName()).lastName(lead.getLastName())
            .phone(lead.getPhone()).email(lead.getEmail())
            .gender(lead.getGender() != null ? Member.Gender.valueOf(lead.getGender().name()) : null)
            .source(lead.getLeadSource()).counsellor(lead.getAssignedTo())
            .notes("Converted from lead. " + (lead.getNotes() != null ? lead.getNotes() : ""))
            .company(lead.getCompany()).branch(lead.getBranch())
            .joinDate(java.time.LocalDate.now()).isActive(true).biometricEnrolled(false)
            .build();
        m = memberRepo.save(m);

        lead.setStatus(Lead.LeadStatus.CONVERTED);
        lead.setConvertedMemberId(m.getId());
        lead.setConvertedAt(LocalDateTime.now());
        leadRepo.save(lead);

        actRepo.save(LeadActivity.builder().lead(lead).type(LeadActivity.ActivityType.STATUS_CHANGE)
            .notes("Converted to member " + m.getMemberCode()).oldStatus("NEGOTIATION").newStatus("CONVERTED").build());

        log.info("Lead {} converted to member {}", lead.getPhone(), m.getMemberCode());
        return toResponse(lead);
    }

    public LeadDashboard getDashboard(UUID branchId) {
        long total = leadRepo.countByBranchId(branchId);
        long newC = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.NEW);
        long contacted = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.CONTACTED);
        long followUp = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.FOLLOW_UP);
        long trial = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.TRIAL);
        long negotiation = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.NEGOTIATION);
        long converted = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.CONVERTED);
        long lost = leadRepo.countByBranchIdAndStatus(branchId, Lead.LeadStatus.LOST);
        double convRate = total > 0 ? Math.round(converted * 1000.0 / total) / 10.0 : 0;

        List<ChartData> statusDist = new ArrayList<>();
        for (Object[] row : leadRepo.countByStatusGrouped(branchId))
            statusDist.add(ChartData.builder().label(row[0].toString()).value((Long)row[1]).build());

        List<ChartData> sourceDist = new ArrayList<>();
        for (Object[] row : leadRepo.countBySourceGrouped(branchId))
            sourceDist.add(ChartData.builder().label(row[0] != null ? row[0].toString() : "Unknown").value((Long)row[1]).build());

        List<CounsellorStats> cStats = new ArrayList<>();
        for (Object[] row : leadRepo.conversionByAssignee(branchId)) {
            long t = (Long)row[1]; long c = (Long)row[2];
            cStats.add(CounsellorStats.builder().name(row[0].toString()).totalLeads(t).converted(c).conversionRate(t > 0 ? Math.round(c * 1000.0 / t) / 10.0 : 0).build());
        }
        cStats.sort((a,b) -> Double.compare(b.getConversionRate(), a.getConversionRate()));

        List<LeadResponse> overdue = leadRepo.findOverdueFollowUps(branchId, LocalDateTime.now()).stream().map(this::toResponse).toList();

        return LeadDashboard.builder().total(total).newCount(newC).contacted(contacted).followUp(followUp)
            .trial(trial).negotiation(negotiation).converted(converted).lost(lost).conversionRate(convRate)
            .statusDistribution(statusDist).sourceDistribution(sourceDist)
            .counsellorStats(cStats).overdueFollowUps(overdue).build();
    }

    public List<LeadActivityResponse> getActivities(UUID leadId) {
        return actRepo.findByLeadIdOrderByCreatedAtDesc(leadId).stream().map(this::toActResponse).toList();
    }

    @Transactional
    public LeadActivityResponse addActivity(UUID leadId, LeadStatusUpdate req) {
        Lead lead = leadRepo.findById(leadId).orElseThrow();
        lead.setLastContacted(LocalDateTime.now());
        lead.setFollowUpCount(lead.getFollowUpCount() + 1);
        if (req.getNextFollowUp() != null) lead.setNextFollowUp(req.getNextFollowUp());
        leadRepo.save(lead);

        LeadActivity.ActivityType type = LeadActivity.ActivityType.NOTE;
        if (req.getCallResponse() != null) type = LeadActivity.ActivityType.CALL;
        LeadActivity act = actRepo.save(LeadActivity.builder().lead(lead).type(type)
            .notes(req.getNotes()).performedBy(req.getPerformedBy()).callResponse(req.getCallResponse())
            .nextFollowUp(req.getNextFollowUp()).build());
        return toActResponse(act);
    }

    private LeadResponse toResponse(Lead l) {
        List<LeadActivityResponse> recent = actRepo.findByLeadIdOrderByCreatedAtDesc(l.getId()).stream()
            .limit(3).map(this::toActResponse).toList();
        return LeadResponse.builder().id(l.getId()).firstName(l.getFirstName()).lastName(l.getLastName())
            .phone(l.getPhone()).email(l.getEmail()).gender(l.getGender() != null ? l.getGender().name() : null)
            .status(l.getStatus().name()).leadSource(l.getLeadSource()).campaignName(l.getCampaignName())
            .referredBy(l.getReferredBy()).assignedTo(l.getAssignedTo())
            .interestedPlan(l.getInterestedPlan()).expectedJoinDate(l.getExpectedJoinDate())
            .nextFollowUp(l.getNextFollowUp()).lastContacted(l.getLastContacted())
            .followUpCount(l.getFollowUpCount()).lostReason(l.getLostReason())
            .convertedMemberId(l.getConvertedMemberId()).convertedAt(l.getConvertedAt())
            .notes(l.getNotes()).createdAt(l.getCreatedAt())
            .recentActivities(recent).build();
    }

    private LeadActivityResponse toActResponse(LeadActivity a) {
        return LeadActivityResponse.builder().id(a.getId()).type(a.getType().name()).notes(a.getNotes())
            .performedBy(a.getPerformedBy()).oldStatus(a.getOldStatus()).newStatus(a.getNewStatus())
            .callResponse(a.getCallResponse()).nextFollowUp(a.getNextFollowUp()).createdAt(a.getCreatedAt()).build();
    }

    private String genCode() {
        String c; do { c = "GF" + String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1,999999)); }
        while (memberRepo.findByMemberCode(c).isPresent()); return c;
    }
}
