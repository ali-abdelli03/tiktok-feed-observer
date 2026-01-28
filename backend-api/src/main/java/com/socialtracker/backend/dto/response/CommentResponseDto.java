package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for comment data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String commentHash;
    private String videoPlatformId;
    
    // Author
    private String authorHandle;
    private String authorName;
    
    // Content
    private String textContent;
    private String imageUrl;
    
    // Engagement
    private Integer likes;
    private Boolean likedByAuthor;
    
    // Mentions
    private List<MentionDto> mentions;
    
    // Timestamp
    private LocalDateTime capturedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MentionDto {
        private String displayName;
        private String encodedUrl;
    }
}
