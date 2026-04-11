package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymManagementService {

    private final CompanyRepository companyRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final MemberRepository memberRepo;
    private final StaffRepository staffRepo;
    private final PasswordEncoder passwordEncoder;

    // ===== COMPANY (GYM) CRUD =====

    public List<CompanyResponse> getAllGyms() {
        return companyRepo.findAll().stream().map(this::toCompanyResponse).toList();
    }

    public CompanyResponse getGym(UUID id) {
        return toCompanyResponse(companyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Gym not found")));
    }

    @Transactional
    public CompanyResponse createGym(CompanyRequest req) {
        // Validate unique code
        if (companyRepo.findByCode(req.getCode().toUpperCase()).isPresent()) {
            throw new RuntimeException("Gym code '" + req.getCode() + "' already exists");
        }

        Company company = Company.builder()
                .name(req.getName())
                .code(req.getCode().toUpperCase())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .logoUrl(req.getLogoUrl())
                .theme(req.getTheme() != null ? req.getTheme() : "orange")
                .isActive(true)
                .build();
        company = companyRepo.save(company);

        // Create an Admin user for this gym if admin credentials provided
        if (req.getAdminEmail() != null && !req.getAdminEmail().isBlank()) {
            if (userRepo.existsByEmail(req.getAdminEmail())) {
                throw new RuntimeException("Admin email '" + req.getAdminEmail() + "' is already registered");
            }
            String pw = (req.getAdminPassword() != null && !req.getAdminPassword().isBlank())
                    ? req.getAdminPassword() : "admin123";

            userRepo.save(User.builder()
                    .company(company)
                    .email(req.getAdminEmail())
                    .passwordHash(passwordEncoder.encode(pw))
                    .role(User.UserRole.ADMIN)
                    .isActive(true)
                    .passwordChanged(false)
                    .build());

            log.info("Created Admin {} for gym {}", req.getAdminEmail(), company.getName());
        }

        log.info("Gym created: {} ({})", company.getName(), company.getCode());
        return toCompanyResponse(company);
    }

    @Transactional
    public CompanyResponse updateGym(UUID id, CompanyRequest req) {
        Company c = companyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Gym not found"));

        if (req.getName() != null) c.setName(req.getName());
        if (req.getEmail() != null) c.setEmail(req.getEmail());
        if (req.getPhone() != null) c.setPhone(req.getPhone());
        if (req.getAddress() != null) c.setAddress(req.getAddress());
        if (req.getLogoUrl() != null) c.setLogoUrl(req.getLogoUrl());
        // Code is immutable after creation

        return toCompanyResponse(companyRepo.save(c));
    }

    @Transactional
    public void deactivateGym(UUID id) {
        Company c = companyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Gym not found"));
        c.setIsActive(false);
        companyRepo.save(c);
        log.info("Gym deactivated: {}", c.getName());
    }

    // ===== BRANCH CRUD =====

    public List<BranchResponse> getBranches(UUID companyId) {
        return branchRepo.findByCompanyIdAndIsActiveTrue(companyId).stream()
                .map(this::toBranchResponse).toList();
    }

    public BranchResponse getBranch(UUID branchId) {
        return toBranchResponse(branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found")));
    }

    @Transactional
    public BranchResponse createBranch(UUID companyId, BranchRequest req) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Gym not found"));

        // Validate unique code
        String code = req.getCode().toUpperCase();
        if (branchRepo.findAll().stream().anyMatch(b -> b.getCode().equalsIgnoreCase(code))) {
            throw new RuntimeException("Branch code '" + code + "' already exists");
        }

        Branch branch = Branch.builder()
                .company(company)
                .name(req.getName())
                .code(code)
                .address(req.getAddress())
                .city(req.getCity())
                .phone(req.getPhone())
                .email(req.getEmail())
                .isActive(true)
                .build();

        branch = branchRepo.save(branch);
        log.info("Branch created: {} ({}) under {}", branch.getName(), branch.getCode(), company.getName());
        return toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse updateBranch(UUID branchId, BranchRequest req) {
        Branch b = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (req.getName() != null) b.setName(req.getName());
        if (req.getAddress() != null) b.setAddress(req.getAddress());
        if (req.getCity() != null) b.setCity(req.getCity());
        if (req.getPhone() != null) b.setPhone(req.getPhone());
        if (req.getEmail() != null) b.setEmail(req.getEmail());

        return toBranchResponse(branchRepo.save(b));
    }

    @Transactional
    public void deactivateBranch(UUID branchId) {
        Branch b = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        b.setIsActive(false);
        branchRepo.save(b);
        log.info("Branch deactivated: {}", b.getName());
    }

    // ===== RESPONSE BUILDERS =====

    private CompanyResponse toCompanyResponse(Company c) {
        List<Branch> branches = branchRepo.findByCompanyIdAndIsActiveTrue(c.getId());
        long memberCount = 0;
        long staffCount = 0;
        for (Branch b : branches) {
            memberCount += memberRepo.countByBranchIdAndIsActiveTrue(b.getId());
            staffCount += staffRepo.countByBranchIdAndIsActiveTrue(b.getId());
        }

        return CompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .code(c.getCode())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .logoUrl(c.getLogoUrl())
                .theme(c.getTheme() != null ? c.getTheme() : "orange")
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .branchCount(branches.size())
                .memberCount(memberCount)
                .staffCount(staffCount)
                .branches(branches.stream().map(this::toBranchResponse).toList())
                .build();
    }

    private BranchResponse toBranchResponse(Branch b) {
        return BranchResponse.builder()
                .id(b.getId())
                .companyId(b.getCompany().getId())
                .companyName(b.getCompany().getName())
                .name(b.getName())
                .code(b.getCode())
                .address(b.getAddress())
                .city(b.getCity())
                .phone(b.getPhone())
                .email(b.getEmail())
                .isActive(b.getIsActive())
                .createdAt(b.getCreatedAt())
                .memberCount(memberRepo.countByBranchIdAndIsActiveTrue(b.getId()))
                .staffCount(staffRepo.countByBranchIdAndIsActiveTrue(b.getId()))
                .build();
    }
}
