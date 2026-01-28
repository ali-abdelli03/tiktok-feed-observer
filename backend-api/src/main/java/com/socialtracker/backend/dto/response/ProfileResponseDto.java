package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for profile data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDto {
    private Long id;
    private String platformHandle;
    private String displayName;
    private String bio;
    private String profileLink;
    private Boolean isVerified;
    
    // Stats
    private Long followerCount;
    private Long followingCount;
    private Long likesCount;
    
    // Aggregated data
    private Integer videoCount;
    private Long totalLikes;
    
    // Timestamps
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastUpdatedAt;
}
