package com.socialtracker.backend.dto;

public record SearchEventDto(
        String scraped_by,  // Tester username
        String type,        // Always "search"
        String query,       // Search query text
        String timestamp,   // ISO timestamp
        String url          // Full URL of the search page
) {
}
