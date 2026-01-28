package com.socialtracker.backend.dto;

/**
 * DTO for watch time events from TikTok Scraper
 * Tracks how long a user watched each video
 */
public record WatchTimeDto(
    // Scraper metadata
    String scraped_by,
    String timestamp,
    String session_id,
    
    // Video info
    String video_id,
    String video_url,
    String author_handle,
    
    // Watch duration in milliseconds
    Long watch_duration_ms
) {}
