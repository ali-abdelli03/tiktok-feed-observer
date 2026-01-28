package com.socialtracker.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for comment data from TikTok Scraper v6.1
 * Simplified structure with all text content in one field
 */
public record CommentDto(
    // Identifiers
    String id,
    String video_id,
    
    // Author info
    String author_handle,
    String author_name,
    
    // All text content collected from the comment (separated by |)
    String all_text,
    
    // Raw text from comment-level-1 element
    String text_comment,
    
    // Image URL if present
    String image_url,
    
    // Number of likes on this comment
    Integer likes,
    
    // Whether this comment was liked by the video author
    Boolean liked_by_author,
    
    // List of mentioned users in the comment
    // Each mention is a map with keys: displayName, encodedUrl, isEncoded
    List<Map<String, Object>> mentions
) {}