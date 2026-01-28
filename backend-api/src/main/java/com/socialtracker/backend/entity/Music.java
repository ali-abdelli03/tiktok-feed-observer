package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Music entity - audio tracks used in videos
 */
@Entity
@Table(name = "music")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Music {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "platform_id", unique = true, length = 100)
    private String platformId;
    
    @Column(length = 500)
    private String name;
    
    @Column(length = 1000)
    private String url;
    
    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;
    
    @PrePersist
    protected void onCreate() {
        firstSeenAt = LocalDateTime.now();
    }
}
