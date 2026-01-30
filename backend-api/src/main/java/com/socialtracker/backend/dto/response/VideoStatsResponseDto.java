package com.socialtracker.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatsResponseDto {
    private Long id;
    private Long videoId;
    private String videoPlatformId;

    // Rinomina questi campi per matchare l'HTML (likeCount, commentCount...)
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Long saveCount;
    private Long playCount; // Aggiunto per completezza

    private String likesRaw;
    private String commentsRaw;
    private String sharesRaw;
    private String savesRaw;

    private LocalDateTime capturedAt;
}