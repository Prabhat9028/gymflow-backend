package com.gymflow.repository;
import com.gymflow.entity.SignagePlaylist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface SignagePlaylistRepository extends JpaRepository<SignagePlaylist, UUID> {
    List<SignagePlaylist> findByBranchIdAndIsActiveTrue(UUID branchId);
    List<SignagePlaylist> findByCompanyIdAndIsActiveTrue(UUID companyId);
}
