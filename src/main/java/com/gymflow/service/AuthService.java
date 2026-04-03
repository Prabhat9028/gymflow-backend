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
        UUID cid = u.getCompany() != null ? u.getCompany().getId() : null;
        UUID bid = u.getBranch() != null ? u.getBranch().getId() : null;
        if (u.getRole() == User.UserRole.SUPER_ADMIN && bid == null && cid != null) {
            var bs = branchRepo.findByCompanyIdAndIsActiveTrue(cid);
            if (!bs.isEmpty()) bid = bs.get(0).getId();
        }
        String token = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name(), cid, bid);
        List<BranchInfo> branches = null;
        if (u.getRole() == User.UserRole.SUPER_ADMIN && cid != null) {
            branches = branchRepo.findByCompanyIdAndIsActiveTrue(cid).stream()
                .map(b -> BranchInfo.builder().id(b.getId()).name(b.getName()).code(b.getCode()).city(b.getCity()).build()).toList();
        }
        String bName = bid != null ? branchRepo.findById(bid).map(Branch::getName).orElse(null) : null;
        String cName = u.getCompany() != null ? u.getCompany().getName() : null;
        return AuthResponse.builder().token(token).email(u.getEmail()).role(u.getRole().name()).userId(u.getId())
            .companyId(cid).companyName(cName).branchId(bid).branchName(bName).branches(branches).build();
    }

    public AuthResponse switchBranch(String email, UUID branchId) {
        User u = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (u.getRole() != User.UserRole.SUPER_ADMIN) throw new RuntimeException("Only Super Admins can switch branches");
        Branch b = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        UUID cid = u.getCompany().getId();
        String token = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name(), cid, branchId);
        List<BranchInfo> branches = branchRepo.findByCompanyIdAndIsActiveTrue(cid).stream()
            .map(br -> BranchInfo.builder().id(br.getId()).name(br.getName()).code(br.getCode()).city(br.getCity()).build()).toList();
        return AuthResponse.builder().token(token).email(u.getEmail()).role(u.getRole().name()).userId(u.getId())
            .companyId(cid).companyName(u.getCompany().getName()).branchId(branchId).branchName(b.getName()).branches(branches).build();
    }
}
