package com.gymflow.repository;
import com.gymflow.entity.BiometricDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface BiometricDeviceRepository extends JpaRepository<BiometricDevice, UUID> {
    Optional<BiometricDevice> findByDeviceSerial(String serial);
    List<BiometricDevice> findByBranchIdAndIsActiveTrue(UUID branchId);
}
