package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    // --- CAMPO FONDAMENTALE MANCANTE ---
    private VideoStatsResponseDto stats;
    // -----------------------------------

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
}