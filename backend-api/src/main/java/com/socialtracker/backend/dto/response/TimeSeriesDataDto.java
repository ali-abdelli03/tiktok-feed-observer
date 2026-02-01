package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Container for time series chart data with multiple datasets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataDto {
    private List<String> labels;              // Time labels for x-axis
    private List<Long> videos;                // Video count per time bucket
    private List<Long> interactions;          // Interaction count per time bucket
    private List<Long> comments;              // Comment count per time bucket
    private List<Long> likes;                 // Likes per time bucket
    private List<Long> saves;                 // Saves per time bucket
}
