package com.gymflow.repository;
import com.gymflow.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByCompanyIdAndIsActiveTrue(UUID companyId);
}
