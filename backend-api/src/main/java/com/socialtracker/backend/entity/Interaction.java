package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Interaction entity - Like/Save/Share actions by the tester
 */
@Entity
@Table(name = "interaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ScrapingSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;
    
    @Column(name = "interaction_type", nullable = false, length = 20)
    private String interactionType;  // 'like', 'save', 'share'
    
    @Column(nullable = false, length = 20)
    private String action;  // 'initial', 'add', 'remove'
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "context_type", length = 50)
    private String contextType;
    
    @Column(name = "performed_at")
    private LocalDateTime performedAt;
    
    @PrePersist
    protected void onCreate() {
        performedAt = LocalDateTime.now();
    }
}
