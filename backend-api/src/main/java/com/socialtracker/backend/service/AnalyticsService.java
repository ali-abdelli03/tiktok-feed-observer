package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.response.DashboardStatsDto;
import com.socialtracker.backend.dto.response.TimeSeriesDataDto;
import com.socialtracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                .totalInteractions(interactionRepository.count())
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
    
    /**
     * Get time-series data for ingestion activity
     * @param range Time range: "24h", "7d", or "30d"
     * @return Time-series data with videos, interactions, and comments per time bucket
     */
    public TimeSeriesDataDto getTimelineData(String range) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime;
        List<String> labels;
        int bucketCount;
        
        // Determine time range and bucket configuration
        switch (range.toLowerCase()) {
            case "24h":
                startTime = endTime.minusHours(24);
                bucketCount = 24;
                labels = generateHourlyLabels(bucketCount);
                break;
            case "7d":
                startTime = endTime.minusDays(7);
                bucketCount = 7;
                labels = generateDailyLabels(bucketCount);
                break;
            case "30d":
                startTime = endTime.minusDays(30);
                bucketCount = 30;
                labels = generateDailyLabels(bucketCount);
                break;
            default:
                startTime = endTime.minusHours(24);
                bucketCount = 24;
                labels = generateHourlyLabels(bucketCount);
        }
        
        // Get time-bucketed data from repositories
        List<Long> videoCounts = getVideoCounts(startTime, endTime, bucketCount, range);
        List<Long> interactionCounts = getInteractionCounts(startTime, endTime, bucketCount, range);
        List<Long> commentCounts = getCommentCounts(startTime, endTime, bucketCount, range);
        List<Long> likeCounts = getLikeCounts(startTime, endTime, bucketCount, range);
        List<Long> saveCounts = getSaveCounts(startTime, endTime, bucketCount, range);
        
        return TimeSeriesDataDto.builder()
                .labels(labels)
                .videos(videoCounts)
                .interactions(interactionCounts)
                .comments(commentCounts)
                .likes(likeCounts)
                .saves(saveCounts)
                .build();
    }
    
    private List<String> generateHourlyLabels(int hours) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:00");
        
        return IntStream.range(0, hours)
                .mapToObj(i -> now.minusHours(hours - 1 - i).format(formatter))
                .collect(Collectors.toList());
    }
    
    private List<String> generateDailyLabels(int days) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        return IntStream.range(0, days)
                .mapToObj(i -> now.minusDays(days - 1 - i).format(formatter))
                .collect(Collectors.toList());
    }
    
    private List<Long> getVideoCounts(LocalDateTime start, LocalDateTime end, int bucketCount, String range) {
        List<Object[]> results = videoRepository.countVideosByTimeBucket(start, end, bucketCount, range);
        return convertToCountList(results, bucketCount);
    }
    
    private List<Long> getInteractionCounts(LocalDateTime start, LocalDateTime end, int bucketCount, String range) {
        List<Object[]> results = interactionRepository.countInteractionsByTimeBucket(start, end, bucketCount, range);
        return convertToCountList(results, bucketCount);
    }
    
    private List<Long> getCommentCounts(LocalDateTime start, LocalDateTime end, int bucketCount, String range) {
        List<Object[]> results = commentRepository.countCommentsByTimeBucket(start, end, bucketCount, range);
        return convertToCountList(results, bucketCount);
    }
    
    private List<Long> getLikeCounts(LocalDateTime start, LocalDateTime end, int bucketCount, String range) {
        List<Object[]> results = interactionRepository.countLikesByTimeBucket(start, end, bucketCount, range);
        return convertToCountList(results, bucketCount);
    }
    
    private List<Long> getSaveCounts(LocalDateTime start, LocalDateTime end, int bucketCount, String range) {
        List<Object[]> results = interactionRepository.countSavesByTimeBucket(start, end, bucketCount, range);
        return convertToCountList(results, bucketCount);
    }
    
    private List<Long> convertToCountList(List<Object[]> results, int bucketCount) {
        // Convert query results to a list with proper bucket ordering
        Map<Integer, Long> bucketMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()
                ));
        
        // Fill in missing buckets with 0
        List<Long> counts = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            counts.add(bucketMap.getOrDefault(i, 0L));
        }
        return counts;
    }
}
