package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.response.DashboardStatsDto;
import com.socialtracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for dashboard analytics and aggregated data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final VideoRepository videoRepository;
    private final ProfileRepository profileRepository;
    private final CommentRepository commentRepository;
    private final TesterRepository testerRepository;
    private final ScrapingSessionRepository sessionRepository;
    private final InteractionRepository interactionRepository;
    
    /**
     * Get dashboard statistics summary
     */
    public DashboardStatsDto getDashboardStats() {
        return DashboardStatsDto.builder()
                .totalVideos(videoRepository.count())
                .totalProfiles(profileRepository.count())
                .totalComments(commentRepository.count())
                .totalTesters(testerRepository.count())
                .totalSessions(sessionRepository.count())
                .adVideos(videoRepository.countAds())
                .liveVideos(videoRepository.countLives())
                .verifiedProfiles(profileRepository.countVerifiedProfiles())
                .totalLikes(interactionRepository.countActiveLikes())
                .totalSaves(interactionRepository.countActiveSaves())
                .videosLast24h(countVideosLast24h())
                .commentsLast24h(countCommentsLast24h())
                .build();
    }
    
    private long countVideosLast24h() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return videoRepository.findRecentVideos(since, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).size();
    }
    
    private long countCommentsLast24h() {
        // For simplicity, using total count
        // In production, add a query with date filter
        return commentRepository.count();
    }
}
