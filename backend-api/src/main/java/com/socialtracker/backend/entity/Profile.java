package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile entity - TikTok user profiles (content creators)
 */
@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "platform_handle", nullable = false, unique = true, length = 100)
    private String platformHandle;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "profile_link", length = 500)
    private String profileLink;
    
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "follower_count")
    private Long followerCount;
    
    @Column(name = "following_count")
    private Long followingCount;
    
    @Column(name = "likes_count")
    private Long likesCount;
    
    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;
    
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Video> videos = new ArrayList<>();
    
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProfileVisit> visits = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        firstSeenAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
