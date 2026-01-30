package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponseDto {
    private Long id;
    private String platformId;
    private String videoUrl;
    private String description;
    private Long duration;

    // Author info
    private String authorHandle;
    private String authorDisplayName;
    private Boolean authorVerified;


    private VideoStatsResponseDto stats;


    // Flags
    private boolean isAd;
    private boolean isLive;
    private Boolean isAi;

    // Music info
    private String musicName;
    private String musicUrl;

    // Timestamps
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastUpdatedAt;

    // Counts (opzionali, usati per liste veloci)
    private Integer commentCount;
    private Integer hashtagCount;

    private Long totalWatchTimeMs;
    private Double averageWatchTimeMs;
    private List<WatchTimeLog> watchHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchTimeLog {
        private LocalDateTime recordedAt;
        private Long durationMs;
        private String testerUsername;
    }
}