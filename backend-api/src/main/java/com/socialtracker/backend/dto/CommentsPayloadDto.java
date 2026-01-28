package com.socialtracker.backend.dto;

import java.util.List;

public record CommentsPayloadDto(
        String scraped_by,          // Tester username
        List<CommentDto> comments,  // Batch of comments
        String timestamp            // ISO timestamp of batch
) {
}
