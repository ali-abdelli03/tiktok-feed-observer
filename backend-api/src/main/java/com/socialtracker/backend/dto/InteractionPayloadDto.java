package com.socialtracker.backend.dto;

import java.util.List;

public record InteractionPayloadDto(
    String scraped_by,              // Tester username
    List<InteractionDto> interactions,  // Batch of interactions
    String timestamp                // ISO timestamp of batch
) {}
