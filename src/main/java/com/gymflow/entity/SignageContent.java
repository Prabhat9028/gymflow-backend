package com.gymflow.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "signage_content")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SignageContent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;

    @Column(nullable = false) private String name;
    @Column(name = "file_name") private String fileName;
    @Column(name = "file_url", length = 1000) private String fileUrl; // S3 URL or local storage
    @Column(name = "thumbnail_url", length = 1000) private String thumbnailUrl;

    @Enumerated(EnumType.STRING) @Column(name = "content_type") private ContentType contentType;
    @Column(name = "mime_type") private String mimeType;
    @Column(name = "file_size") private Long fileSize; // bytes
    @Column(name = "duration_seconds") private Integer durationSeconds; // for video
    @Column(name = "width") private Integer width;
    @Column(name = "height") private Integer height;

    @Column(name = "checksum") private String checksum; // MD5 for offline cache validation
    @Column(name = "is_active") @Builder.Default private Boolean isActive = true;

    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public enum ContentType { VIDEO, IMAGE }
}
