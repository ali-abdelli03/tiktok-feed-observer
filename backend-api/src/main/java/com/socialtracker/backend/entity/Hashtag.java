package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Hashtag entity - tags used in video descriptions
 */
@Entity
@Table(name = "hashtag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 200)
    private String tag;
    
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 1;
    
    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;
    
    @PrePersist
    protected void onCreate() {
        firstSeenAt = LocalDateTime.now();
    }
    
    public void incrementUsageCount() {
        this.usageCount++;
    }
}
