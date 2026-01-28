package com.socialtracker.backend.dto;

/**
 * DTO for profile visit data from TikTok Scraper v6.1
 * Tracks when user visits other TikTok profiles
 */
public record ProfileVisitDto(
    // Scraper metadata
    String scraped_by,
    String timestamp,
    
    // Profile identifiers
    String profile_handle,
    String profile_url,
    
    // Profile metadata
    String display_name,
    String follower_count,
    String following_count,
    String likes_count,
    String bio,
    String profile_link,
    Boolean is_verified,
    
    // Context
    String context_type,
    String referrer_url,
    
    // Visit timestamp
    String visited_at
) {}
