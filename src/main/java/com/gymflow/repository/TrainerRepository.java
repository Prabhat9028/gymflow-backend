package com.gymflow.repository;

import com.gymflow.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, UUID> {
    List<Trainer> findByIsActiveTrue();
    long countByIsActiveTrue();
}
