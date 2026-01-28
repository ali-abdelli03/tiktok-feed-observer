package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for scraping session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDto {
    private Long id;
    private UUID sessionUuid;
    private String testerUsername;
    
    // Counts
    private Integer videoCount;
    private Integer commentCount;
    private Integer interactionCount;
    
    // Timestamps
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    
    // Computed
    private Long durationMinutes;
}
