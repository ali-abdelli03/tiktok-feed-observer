package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.ContentDto;
import com.socialtracker.backend.dto.response.VideoResponseDto;
import com.socialtracker.backend.dto.response.VideoStatsResponseDto;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.repository.VideoRepository;
import com.socialtracker.backend.repository.VideoStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Propagation;
/**
 * Service for managing videos and their stats
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final ProfileService profileService;
    private final MediaService mediaService;
    private final SessionService sessionService;
    @Autowired
    @Lazy
    private VideoService self;
    /**
     * Ingest a video from the scraper (main entry point)
     */
    /**
     * Ingest a video from the scraper (main entry point)
     */
    @Transactional
    public Video ingestVideo(ContentDto dto, ScrapingSession session) {
        String platformId = dto.video_id();

//        // 1. Log to console via ContentService
//        try {
//            contentService.processContent(dto);
//        } catch (Exception e) {
//            log.warn("Logging error (non-critical): {}", e.getMessage());
//        }

        if (platformId == null || platformId.isBlank()) {
            log.warn("Received video without platform ID");
            return null;
        }

        Video video;

        // Use the same robust strategy for ingestion
        Optional<Video> existing = videoRepository.findByPlatformId(platformId);

        if (existing.isPresent()) {
            video = existing.get();
        } else {
            // Even here, we can use the safe creation method
            try {
                video = self.createSkeletonSafely(platformId);
            } catch (Exception e) {
                log.info("Concurrency conflict during ingestion resolved: {}", platformId);
                video = videoRepository.findByPlatformId(platformId)
                        .orElseThrow(() -> new RuntimeException("Critical recovery failure: " + platformId));
            }
        }

        // Update metadata on the secured video object
        updateVideoMetadata(video, dto);
        createStatsSnapshot(video, dto, session);

        if (session != null) {
            sessionService.incrementVideoCount(session);
        }

        return videoRepository.save(video);
    }

    private Video createNewVideo(ContentDto dto) {
        return Video.builder()
                .platformId(dto.video_id())
                .build();
    }
    
    private void updateVideoMetadata(Video video, ContentDto dto) {
        // Author/Profile
        if (dto.author_handle() != null && !dto.author_handle().isBlank()) {
            Profile profile = profileService.findOrCreateProfile(dto.author_handle());
            if (profile != null) {
                video.setProfile(profile);
                if (dto.is_verified() != null && dto.is_verified()) {
                    profile.setIsVerified(true);
                }
            }
        }
        
        // Content
        video.setDescription(dto.description());
        video.setVideoUrl(dto.video_url());
        
        // Flags
        video.setIsAd(Boolean.TRUE.equals(dto.is_ad()));
        video.setIsLive(Boolean.TRUE.equals(dto.is_live()));
        video.setIsAi(Boolean.TRUE.equals(dto.is_ai()));
        
        // Music
        Music music = mediaService.findOrCreateMusic(
                dto.music_id(), 
                dto.music_name(), 
                dto.music_url()
        );
        video.setMusic(music);
        
        // Effect
        Effect effect = mediaService.findOrCreateEffect(
                dto.effect_id(),
                dto.effect_name(),
                dto.effect_url()
        );
        video.setEffect(effect);
        
        // Place
        Place place = mediaService.findOrCreatePlace(
                dto.place_id(),
                dto.place()
        );
        video.setPlace(place);
        
        // Hashtags
        parseAndAddHashtags(video, dto.hashtags());
    }
    
    private void parseAndAddHashtags(Video video, String hashtagsStr) {
        if (hashtagsStr == null || hashtagsStr.isBlank()) {
            return;
        }
        
        // Parse comma-separated or space-separated hashtags
        String[] tags = hashtagsStr.split("[,\\s]+");
        for (String tag : tags) {
            if (!tag.isBlank()) {
                Hashtag hashtag = mediaService.findOrCreateHashtag(tag);
                if (hashtag != null) {
                    video.addHashtag(hashtag);
                }
            }
        }
    }
    /**
     * Finds a video or creates a "skeleton" (placeholder) if it doesn't exist.
     * Used by BehaviorService/CommentService to handle data arriving before the main video ingestion.
     */
    @Transactional
    public Video findOrCreateSkeleton(String platformId) {
        // 1. Fast Check: Try to find it normally
        Optional<Video> existing = videoRepository.findByPlatformId(platformId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. If not found, delegate creation to an ISOLATED transaction
        try {
            return self.createSkeletonSafely(platformId);
        } catch (Exception e) {
            // 3. CATCH: If we are here, another thread created the video milliseconds before us.
            // Because we used REQUIRES_NEW, the current Hibernate session is CLEAN (no bad entities).
            log.info("Race condition resolved for video skeleton: {}", platformId);

            return videoRepository.findByPlatformId(platformId)
                    .orElseThrow(() -> new RuntimeException("Critical: Could not recover video " + platformId + " after concurrency conflict"));
        }
    }
    /**
     * Helper method to create a skeleton in an ISOLATED transaction.
     * Propagation.REQUIRES_NEW suspends the current transaction and starts a fresh one.
     * If this fails (Duplicate Key), it rolls back cleanly without polluting the main session.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Video createSkeletonSafely(String platformId) {
        log.info("⚡ Creating skeleton video (Isolated): {}", platformId);
        Video skeleton = Video.builder()
                .platformId(platformId)
                .firstSeenAt(LocalDateTime.now())
                .build();
        // saveAndFlush forces immediate DB execution to catch constraints early
        return videoRepository.saveAndFlush(skeleton);
    }
    private void createStatsSnapshot(Video video, ContentDto dto, ScrapingSession session) {
        VideoStats stats = VideoStats.builder()
                .video(video)
                .session(session)
                .likes(parseStatValue(dto.stat_likes()))
                .comments(parseStatValue(dto.stat_comments()))
                .shares(parseStatValue(dto.stat_shared()))
                .saves(parseStatValue(dto.stat_saved()))
                .likesRaw(dto.stat_likes_raw())
                .commentsRaw(dto.stat_comments_raw())
                .sharesRaw(dto.stat_shared_raw())
                .savesRaw(dto.stat_saved_raw())
                .build();
        
        videoStatsRepository.save(stats);
    }
    
    /**
     * Parse stat strings like "1.2K", "500", "1M" to Long
     */
    private Long parseStatValue(String statStr) {
        if (statStr == null || statStr.isBlank()) {
            return 0L;
        }
        
        try {
            String cleaned = statStr.trim().toUpperCase().replace(",", "");
            
            double multiplier = 1;
            if (cleaned.endsWith("K")) {
                multiplier = 1_000;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("M")) {
                multiplier = 1_000_000;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("B")) {
                multiplier = 1_000_000_000;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            
            double value = Double.parseDouble(cleaned);
            return (long) (value * multiplier);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    // ========== QUERY METHODS ==========
    
    public Optional<Video> findByPlatformId(String platformId) {
        return videoRepository.findByPlatformId(platformId);
    }
    
    public Page<Video> findAll(Pageable pageable) {
        return videoRepository.findAllOrderByFirstSeenAtDesc(pageable);
    }
    
    public List<Video> findRecentVideos(int hours, int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return videoRepository.findRecentVideos(since, PageRequest.of(0, limit));
    }
    
    public Page<Video> findTopByLikes(Pageable pageable) {
        return videoRepository.findTopByLikes(pageable);
    }
    
    public List<Video> findByHashtag(String tag) {
        return videoRepository.findByHashtag(tag);
    }
    
    public List<Video> searchByDescription(String keyword) {
        return videoRepository.searchByDescription(keyword);
    }
    
    public long count() {
        return videoRepository.count();
    }
    
    public long countAds() {
        return videoRepository.countAds();
    }
    
    public long countLives() {
        return videoRepository.countLives();
    }
    
    // ========== STATS METHODS ==========
    
    public List<VideoStats> getStatsHistory(Long videoId) {
        return videoStatsRepository.findByVideoIdOrderByCapturedAtDesc(videoId);
    }
    
    public Optional<VideoStats> getLatestStats(Long videoId) {
        return videoStatsRepository.findLatestByVideoId(videoId);
    }
    
    // ========== DTO CONVERSION ==========
    
    public VideoResponseDto toDto(Video video) {
        Optional<VideoStats> latestStats = videoStatsRepository.findLatestByVideoId(video.getId());
        
        VideoResponseDto.VideoResponseDtoBuilder builder = VideoResponseDto.builder()
                .id(video.getId())
                .platformId(video.getPlatformId())
                .videoUrl(video.getVideoUrl())
                .description(video.getDescription())
                .isAd(video.getIsAd())
                .isLive(video.getIsLive())
                .isAi(video.getIsAi())
                .firstSeenAt(video.getFirstSeenAt())
                .lastUpdatedAt(video.getLastUpdatedAt())
                .commentCount(video.getComments() != null ? video.getComments().size() : 0)
                .hashtagCount(video.getHashtags() != null ? video.getHashtags().size() : 0);
        
        // Author info
        if (video.getProfile() != null) {
            builder.authorHandle(video.getProfile().getPlatformHandle())
                    .authorDisplayName(video.getProfile().getDisplayName())
                    .authorVerified(video.getProfile().getIsVerified());
        }
        
        // Music info
        if (video.getMusic() != null) {
            builder.musicName(video.getMusic().getName())
                    .musicUrl(video.getMusic().getUrl());
        }
        
        // Latest stats
        latestStats.ifPresent(stats -> builder
                .likes(stats.getLikes())
                .comments(stats.getComments())
                .shares(stats.getShares())
                .saves(stats.getSaves())
        );
        
        return builder.build();
    }
    
    public VideoStatsResponseDto toStatsDto(VideoStats stats) {
        return VideoStatsResponseDto.builder()
                .id(stats.getId())
                .videoId(stats.getVideo().getId())
                .videoPlatformId(stats.getVideo().getPlatformId())
                .likes(stats.getLikes())
                .comments(stats.getComments())
                .shares(stats.getShares())
                .saves(stats.getSaves())
                .likesRaw(stats.getLikesRaw())
                .commentsRaw(stats.getCommentsRaw())
                .sharesRaw(stats.getSharesRaw())
                .savesRaw(stats.getSavesRaw())
                .capturedAt(stats.getCapturedAt())
                .build();
    }
}
