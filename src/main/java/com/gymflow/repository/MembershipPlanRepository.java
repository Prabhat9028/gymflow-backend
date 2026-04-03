package com.gymflow.repository;
import com.gymflow.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {
    List<MembershipPlan> findByBranchIdAndIsActiveTrue(UUID branchId);
}
