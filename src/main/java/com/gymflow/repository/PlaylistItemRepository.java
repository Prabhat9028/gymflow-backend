package com.gymflow.repository;
import com.gymflow.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, UUID> {
    List<PlaylistItem> findByPlaylistIdOrderBySortOrderAsc(UUID playlistId);
    void deleteByPlaylistId(UUID playlistId);
}
