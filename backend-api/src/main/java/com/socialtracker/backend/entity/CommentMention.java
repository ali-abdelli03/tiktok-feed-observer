package com.socialtracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * CommentMention entity - users mentioned in comments
 */
@Entity
@Table(name = "comment_mention")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentMention {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(name = "encoded_url", length = 500)
    private String encodedUrl;
}
