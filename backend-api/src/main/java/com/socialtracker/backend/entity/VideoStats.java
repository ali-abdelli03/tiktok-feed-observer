package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * VideoStats entity - historicized engagement metrics
 * Always INSERT, never UPDATE - keeps history of stat changes
 */
@Entity
@Table(name = "video_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ScrapingSession session;
    
    @Column
    @Builder.Default
    private Long likes = 0L;
    
    @Column
    @Builder.Default
    private Long comments = 0L;
    
    @Column
    @Builder.Default
    private Long shares = 0L;
    
    @Column
    @Builder.Default
    private Long saves = 0L;
    
    // Raw display strings for debugging
    @Column(name = "likes_raw", length = 50)
    private String likesRaw;
    
    @Column(name = "comments_raw", length = 50)
    private String commentsRaw;
    
    @Column(name = "shares_raw", length = 50)
    private String sharesRaw;
    
    @Column(name = "saves_raw", length = 50)
    private String savesRaw;
    
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
    
    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }
}
