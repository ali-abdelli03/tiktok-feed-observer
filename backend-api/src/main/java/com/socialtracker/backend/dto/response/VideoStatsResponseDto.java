package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for video stats history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatsResponseDto {
    private Long id;
    private Long videoId;
    private String videoPlatformId;
    
    // Stats
    private Long likes;
    private Long comments;
    private Long shares;
    private Long saves;
    
    // Raw values
    private String likesRaw;
    private String commentsRaw;
    private String sharesRaw;
    private String savesRaw;
    
    // Timestamp
    private LocalDateTime capturedAt;
}
