package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single point in a time series chart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPointDto {
    private String label;      // Time label (e.g., "00:00", "2024-01-15", "Week 1")
    private Long value;        // Data value for this time point
}
