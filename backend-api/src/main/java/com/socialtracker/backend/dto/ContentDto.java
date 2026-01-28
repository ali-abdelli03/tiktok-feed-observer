package com.socialtracker.backend.dto;

/**
 * DTO for video/content data from TikTok Scraper v6.1
 * Supports multi-context architecture with raw stat values
 */
public record ContentDto(
        // Scraper metadata
        String scraped_by,
        String timestamp,
        
        // Author info
        String author_handle,
        Boolean is_verified,
        
        // Content flags
        Boolean is_ad,
        Boolean is_live,
        Boolean is_ai,
        
        // Content
        String description,
        String hashtags,
        String mentions,
        
        // Place
        String place,
        String place_id,
        
        // Stats - parsed values
        String stat_likes,
        String stat_comments,
        String stat_saved,
        String stat_shared,
        
        // Stats - raw display strings (v6.1)
        String stat_likes_raw,
        String stat_comments_raw,
        String stat_saved_raw,
        String stat_shared_raw,
        
        // Music
        String music_name,
        String music_id,
        String music_url,
        
        // Effect
        String effect_name,
        String effect_id,
        String effect_url,
        
        // Video identifiers
        String video_id,
        String video_url,
        
        // Session tracking
        Integer session_sequence,
        
        // Context type: FOR_YOU, PROFILE
        String context_type
) {}