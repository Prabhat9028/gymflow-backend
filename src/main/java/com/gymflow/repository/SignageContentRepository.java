package com.gymflow.repository;
import com.gymflow.entity.SignageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface SignageContentRepository extends JpaRepository<SignageContent, UUID> {
    List<SignageContent> findByBranchIdAndIsActiveTrue(UUID branchId);
    List<SignageContent> findByCompanyIdAndIsActiveTrue(UUID companyId);
}
