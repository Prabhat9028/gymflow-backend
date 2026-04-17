package com.gymflow.repository;
import com.gymflow.entity.SignageDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SignageDeviceRepository extends JpaRepository<SignageDevice, UUID> {
    List<SignageDevice> findByBranchIdAndIsActiveTrue(UUID branchId);
    List<SignageDevice> findByCompanyIdAndIsActiveTrue(UUID companyId);
    Optional<SignageDevice> findByDeviceCode(String deviceCode);
    Optional<SignageDevice> findByDeviceId(String deviceId);
}
