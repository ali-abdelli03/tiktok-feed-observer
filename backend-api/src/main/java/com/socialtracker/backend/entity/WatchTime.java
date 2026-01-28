package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * WatchTime entity - how long the tester watched each video
 */
@Entity
@Table(name = "watch_time")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchTime {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ScrapingSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;
    
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
