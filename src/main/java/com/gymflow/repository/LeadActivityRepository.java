package com.gymflow.repository;
import com.gymflow.entity.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {
    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
