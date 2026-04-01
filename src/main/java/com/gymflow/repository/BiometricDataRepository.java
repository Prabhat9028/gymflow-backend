package com.gymflow.repository;

import com.gymflow.entity.BiometricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BiometricDataRepository extends JpaRepository<BiometricData, UUID> {
    List<BiometricData> findByMemberIdAndIsActiveTrue(UUID memberId);
    Optional<BiometricData> findByMemberIdAndBiometricType(UUID memberId, BiometricData.BiometricType type);

    @Query("SELECT b FROM BiometricData b WHERE b.biometricType = :type AND b.isActive = true")
    List<BiometricData> findAllActiveByType(@Param("type") BiometricData.BiometricType type);

    Optional<BiometricData> findByTemplateHash(String hash);
    boolean existsByMemberIdAndBiometricTypeAndIsActiveTrue(UUID memberId, BiometricData.BiometricType type);
}
