package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comment entity - comments on videos
 */
@Entity
@Table(name = "comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "comment_hash", nullable = false, unique = true, length = 100)
    private String commentHash;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
    
    @Column(name = "author_handle", length = 100)
    private String authorHandle;
    
    @Column(name = "author_name", length = 255)
    private String authorName;
    
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column
    @Builder.Default
    private Integer likes = 0;
    
    @Column(name = "liked_by_author")
    @Builder.Default
    private Boolean likedByAuthor = false;
    
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
    
    // Relationships
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentMention> mentions = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }
    
    public void addMention(CommentMention mention) {
        mentions.add(mention);
        mention.setComment(this);
    }
}
