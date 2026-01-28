package com.socialtracker.backend.dto;

/**
 * DTO for user interaction data from TikTok Scraper v6.1
 * Tracks likes, saves, and shares with context awareness
 */
public record InteractionDto(
    String video_id,
    String type,           // "like", "save", or "share"
    Boolean is_active,     // true = active, false = inactive
    String action,         // "initial", "add", or "remove"
    String timestamp,
    
    // Context (v6.1)
    String context_type    // FOR_YOU, PROFILE, VIDEO_SINGLE, VIDEO_MIXED
) {}
