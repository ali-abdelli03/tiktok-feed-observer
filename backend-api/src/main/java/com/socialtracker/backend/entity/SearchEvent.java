package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SearchEvent entity - search queries made by the tester
 */
@Entity
@Table(name = "search_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ScrapingSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tester_id")
    private Tester tester;
    
    @Column(nullable = false, length = 500)
    private String query;
    
    @Column(length = 1000)
    private String url;
    
    @Column(name = "searched_at")
    private LocalDateTime searchedAt;
    
    @PrePersist
    protected void onCreate() {
        searchedAt = LocalDateTime.now();
    }
}
