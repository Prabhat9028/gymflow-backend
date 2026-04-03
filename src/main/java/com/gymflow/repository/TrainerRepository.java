package com.gymflow.repository;
import com.gymflow.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface TrainerRepository extends JpaRepository<Trainer, UUID> {
    List<Trainer> findByBranchIdAndIsActiveTrue(UUID branchId);
    long countByBranchIdAndIsActiveTrue(UUID branchId);
}
