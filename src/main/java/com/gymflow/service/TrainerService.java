package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.Trainer;
import com.gymflow.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerRepository trainerRepository;

    public TrainerResponse createTrainer(TrainerRequest req) {
        Trainer t = Trainer.builder()
                .firstName(req.getFirstName()).lastName(req.getLastName())
                .email(req.getEmail()).phone(req.getPhone())
                .specialization(req.getSpecialization())
                .certification(req.getCertification())
                .hourlyRate(req.getHourlyRate())
                .isActive(true).build();
        return toResponse(trainerRepository.save(t));
    }

    public List<TrainerResponse> getAllTrainers() {
        return trainerRepository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }

    public TrainerResponse getTrainer(UUID id) {
        return toResponse(trainerRepository.findById(id).orElseThrow(() -> new RuntimeException("Trainer not found")));
    }

    public TrainerResponse updateTrainer(UUID id, TrainerRequest req) {
        Trainer t = trainerRepository.findById(id).orElseThrow(() -> new RuntimeException("Trainer not found"));
        if (req.getFirstName() != null) t.setFirstName(req.getFirstName());
        if (req.getLastName() != null) t.setLastName(req.getLastName());
        if (req.getEmail() != null) t.setEmail(req.getEmail());
        if (req.getPhone() != null) t.setPhone(req.getPhone());
        if (req.getSpecialization() != null) t.setSpecialization(req.getSpecialization());
        if (req.getCertification() != null) t.setCertification(req.getCertification());
        if (req.getHourlyRate() != null) t.setHourlyRate(req.getHourlyRate());
        return toResponse(trainerRepository.save(t));
    }

    public void deleteTrainer(UUID id) {
        Trainer t = trainerRepository.findById(id).orElseThrow(() -> new RuntimeException("Trainer not found"));
        t.setIsActive(false);
        trainerRepository.save(t);
    }

    public long countActive() { return trainerRepository.countByIsActiveTrue(); }

    private TrainerResponse toResponse(Trainer t) {
        return TrainerResponse.builder()
                .id(t.getId()).firstName(t.getFirstName()).lastName(t.getLastName())
                .email(t.getEmail()).phone(t.getPhone())
                .specialization(t.getSpecialization()).certification(t.getCertification())
                .hourlyRate(t.getHourlyRate()).isActive(t.getIsActive()).build();
    }
}
