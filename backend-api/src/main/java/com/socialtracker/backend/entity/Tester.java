package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tester entity - represents users who run the scraper extension
 */
@Entity
@Table(name = "tester")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tester {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    // Relationships
    @OneToMany(mappedBy = "tester", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScrapingSession> sessions = new ArrayList<>();
    
    @OneToMany(mappedBy = "tester", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SearchEvent> searchEvents = new ArrayList<>();
    
    @OneToMany(mappedBy = "tester", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProfileVisit> profileVisits = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActiveAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastActiveAt = LocalDateTime.now();
    }
}
