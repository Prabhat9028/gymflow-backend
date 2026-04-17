package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "signage_playlist_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaylistItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "playlist_id", nullable = false) private SignagePlaylist playlist;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "content_id", nullable = false) private SignageContent content;

    @Column(name = "sort_order") private Integer sortOrder;
    @Column(name = "display_duration") private Integer displayDuration; // seconds — for images; videos use their own duration
    @Column(name = "transition_type") @Builder.Default private String transitionType = "fade"; // fade, slide, none

    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
}
