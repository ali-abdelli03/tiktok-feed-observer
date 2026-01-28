package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard statistics summary DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    // Counts
    private long totalVideos;
    private long totalProfiles;
    private long totalComments;
    private long totalTesters;
    private long totalSessions;
    
    // Content breakdown
    private long adVideos;
    private long liveVideos;
    private long verifiedProfiles;
    
    // Interactions
    private long totalLikes;
    private long totalSaves;
    
    // Recent activity
    private long videosLast24h;
    private long commentsLast24h;
}
