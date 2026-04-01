package com.gymflow.repository;

import com.gymflow.entity.GymClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GymClassRepository extends JpaRepository<GymClass, UUID> {
    List<GymClass> findByIsActiveTrue();
    List<GymClass> findByTrainerIdAndIsActiveTrue(UUID trainerId);
}
