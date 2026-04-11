package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import com.gymflow.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest req) {
        User u = userRepo.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!encoder.matches(req.getPassword(), u.getPasswordHash())) throw new RuntimeException("Invalid credentials");
        if (!u.getIsActive()) throw new RuntimeException("Account disabled");
        return buildResponse(u);
    }

    public AuthResponse switchBranch(String email, UUID branchId) {
        User u = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (u.getRole() != User.UserRole.SUPER_ADMIN && u.getRole() != User.UserRole.ADMIN)
            throw new RuntimeException("Only Super Admins and Admins can switch branches");
        Branch b = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        if (u.getCompany() != null && !u.getCompany().getId().equals(b.getCompany().getId()))
            throw new RuntimeException("Cannot switch to branch of another company");
        return buildResponseWithBranch(u, branchId);
    }

    public AuthResponse changePassword(String email, ChangePasswordRequest req) {
        User u = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (!encoder.matches(req.getCurrentPassword(), u.getPasswordHash()))
            throw new RuntimeException("Current password is incorrect");
        u.setPasswordHash(encoder.encode(req.getNewPassword()));
        u.setPasswordChanged(true);
        userRepo.save(u);
        return buildResponse(u);
    }

    private AuthResponse buildResponse(User u) {
        UUID cid = u.getCompany() != null ? u.getCompany().getId() : null;
        UUID bid = u.getBranch() != null ? u.getBranch().getId() : null;
        if (bid == null && cid != null && (u.getRole() == User.UserRole.SUPER_ADMIN || u.getRole() == User.UserRole.ADMIN)) {
            var bs = branchRepo.findByCompanyIdAndIsActiveTrue(cid);
            if (!bs.isEmpty()) bid = bs.get(0).getId();
        }
        String token = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name(), cid, bid);
        List<BranchInfo> branches = null;
        if (cid != null && (u.getRole() == User.UserRole.SUPER_ADMIN || u.getRole() == User.UserRole.ADMIN)) {
            branches = branchRepo.findByCompanyIdAndIsActiveTrue(cid).stream()
                .map(b -> BranchInfo.builder().id(b.getId()).name(b.getName()).code(b.getCode()).city(b.getCity()).build()).toList();
        }
        String bName = bid != null ? branchRepo.findById(bid).map(Branch::getName).orElse(null) : null;
        String cName = u.getCompany() != null ? u.getCompany().getName() : null;
        String cLogo = u.getCompany() != null ? u.getCompany().getLogoUrl() : null;
        String theme = u.getCompany() != null && u.getCompany().getTheme() != null ? u.getCompany().getTheme() : "orange";
        return AuthResponse.builder().token(token).email(u.getEmail()).role(u.getRole().name()).userId(u.getId())
            .companyId(cid).companyName(cName).companyLogo(cLogo).branchId(bid).branchName(bName).branches(branches)
            .passwordChanged(u.getPasswordChanged() != null ? u.getPasswordChanged() : false).theme(theme).build();
    }

    private AuthResponse buildResponseWithBranch(User u, UUID branchId) {
        UUID cid = u.getCompany().getId();
        Branch b = branchRepo.findById(branchId).orElseThrow();
        String token = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name(), cid, branchId);
        List<BranchInfo> branches = branchRepo.findByCompanyIdAndIsActiveTrue(cid).stream()
            .map(br -> BranchInfo.builder().id(br.getId()).name(br.getName()).code(br.getCode()).city(br.getCity()).build()).toList();
        String theme = u.getCompany() != null && u.getCompany().getTheme() != null ? u.getCompany().getTheme() : "orange";
        return AuthResponse.builder().token(token).email(u.getEmail()).role(u.getRole().name()).userId(u.getId())
            .companyId(cid).companyName(u.getCompany().getName()).companyLogo(u.getCompany().getLogoUrl())
            .branchId(branchId).branchName(b.getName()).branches(branches)
            .passwordChanged(u.getPasswordChanged() != null ? u.getPasswordChanged() : false).theme(theme).build();
    }
}
