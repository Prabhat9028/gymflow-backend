package com.gymflow.service;
import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class TrainerService {
    private final TrainerRepository repo;
    private final BranchRepository branchRepo;
    public TrainerResponse create(TrainerRequest req, UUID branchId, UUID companyId) {
        Trainer t = Trainer.builder().firstName(req.getFirstName()).lastName(req.getLastName()).email(req.getEmail()).phone(req.getPhone())
            .specialization(req.getSpecialization()).certification(req.getCertification()).hourlyRate(req.getHourlyRate())
            .branch(new Branch(){{ setId(branchId); }}).company(new Company(){{ setId(companyId); }}).isActive(true).build();
        return toR(repo.save(t));
    }
    public List<TrainerResponse> getAll(UUID branchId) { return repo.findByBranchIdAndIsActiveTrue(branchId).stream().map(this::toR).toList(); }
    public long countActive(UUID branchId) { return repo.countByBranchIdAndIsActiveTrue(branchId); }
    public void delete(UUID id) { Trainer t = repo.findById(id).orElseThrow(); t.setIsActive(false); repo.save(t); }
    private TrainerResponse toR(Trainer t) { return TrainerResponse.builder().id(t.getId()).firstName(t.getFirstName()).lastName(t.getLastName()).email(t.getEmail()).phone(t.getPhone()).specialization(t.getSpecialization()).certification(t.getCertification()).hourlyRate(t.getHourlyRate()).isActive(t.getIsActive()).build(); }
}
