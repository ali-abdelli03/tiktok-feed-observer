package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ScrapingSession entity - tracks each scraping session
 */
@Entity
@Table(name = "scraping_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_uuid", nullable = false, unique = true)
    private UUID sessionUuid;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tester_id", nullable = false)
    private Tester tester;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "video_count")
    @Builder.Default
    private Integer videoCount = 0;
    
    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;
    
    @Column(name = "interaction_count")
    @Builder.Default
    private Integer interactionCount = 0;
    
    // Relationships
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @Builder.Default
    private List<VideoStats> videoStats = new ArrayList<>();
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Interaction> interactions = new ArrayList<>();
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @Builder.Default
    private List<WatchTime> watchTimes = new ArrayList<>();
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SearchEvent> searchEvents = new ArrayList<>();
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProfileVisit> profileVisits = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        if (sessionUuid == null) {
            sessionUuid = UUID.randomUUID();
        }
    }
    
    public void incrementVideoCount() {
        this.videoCount++;
    }
    
    public void incrementCommentCount() {
        this.commentCount++;
    }
    
    public void incrementInteractionCount() {
        this.interactionCount++;
    }
}
