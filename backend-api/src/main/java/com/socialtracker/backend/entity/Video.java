package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Video entity - main video content table
 */
@Entity
@Table(name = "video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "platform_id", nullable = false, unique = true, length = 100)
    private String platformId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile profile;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id")
    private Music music;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effect_id")
    private Effect effect;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL)
    private List<VideoStats> videoStats;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "video_url", length = 1000)
    private String videoUrl;
    
    @Column(name = "is_ad")
    @Builder.Default
    private Boolean isAd = false;
    
    @Column(name = "is_live")
    @Builder.Default
    private Boolean isLive = false;
    
    @Column(name = "is_ai")
    @Builder.Default
    private Boolean isAi = false;
    
    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;
    
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
    
    // Relationships
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "video_hashtag",
        joinColumns = @JoinColumn(name = "video_id"),
        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    private Set<Hashtag> hashtags = new HashSet<>();
    
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoStats> stats = new ArrayList<>();
    
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Interaction> interactions = new ArrayList<>();
    
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL)
    @Builder.Default
    private List<WatchTime> watchTimes = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        firstSeenAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
    
    public void addHashtag(Hashtag hashtag) {
        hashtags.add(hashtag);
    }
    
    public void addStats(VideoStats videoStats) {
        stats.add(videoStats);
        videoStats.setVideo(this);
    }
    
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setVideo(this);
    }
    public java.util.List<VideoStats> getVideoStats() {
        return this.videoStats;
    }

    public void setVideoStats(java.util.List<VideoStats> videoStats) {
        this.videoStats = videoStats;
    }
}
