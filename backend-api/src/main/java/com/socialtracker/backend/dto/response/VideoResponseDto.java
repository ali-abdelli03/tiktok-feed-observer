package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for video data with latest stats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponseDto {
    private Long id;
    private String platformId;
    private String videoUrl;
    private String description;
    
    // Author info
    private String authorHandle;
    private String authorDisplayName;
    private Boolean authorVerified;
    
    // Flags
    private Boolean isAd;
    private Boolean isLive;
    private Boolean isAi;
    
    // Latest stats
    private Long likes;
    private Long comments;
    private Long shares;
    private Long saves;
    
    // Music info
    private String musicName;
    private String musicUrl;
    
    // Timestamps
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastUpdatedAt;
    
    // Counts
    private Integer commentCount;
    private Integer hashtagCount;
}
