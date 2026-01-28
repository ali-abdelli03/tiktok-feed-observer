package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ProfileVisit entity - when the tester visits a profile page
 */
@Entity
@Table(name = "profile_visit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileVisit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ScrapingSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tester_id")
    private Tester tester;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile profile;
    
    @Column(name = "context_type", length = 50)
    private String contextType;
    
    @Column(name = "referrer_url", length = 1000)
    private String referrerUrl;
    
    @Column(name = "visited_at")
    private LocalDateTime visitedAt;
    
    @PrePersist
    protected void onCreate() {
        visitedAt = LocalDateTime.now();
    }
}
