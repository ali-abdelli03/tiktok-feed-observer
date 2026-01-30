package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.*;
import com.socialtracker.backend.dto.response.ApiResponse;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for ingesting data from the TikTok scraper extension.
 * Handles both LOGGING to console (via ContentService) and SAVING to DB.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IngestionController {

    // DB Services
    private final VideoService videoService;
    private final CommentService commentService;
    private final ProfileService profileService;
    private final BehaviorService behaviorService;
    private final SessionService sessionService;
    private final TesterService testerService;

    // Logging Service (ex ContentController logic)
    private final ContentService contentService;

    /**
     * Ingest video/content data
     */
    @PostMapping("/tiktok")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestVideo(
            @RequestBody ContentDto request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        // 1. PRINT LOGS (Console)
        try {
            contentService.processContent(request);
        } catch (Exception e) {
            log.warn("Console logging failed: {}", e.getMessage());
        }

        // 2. SAVE TO DB
        try {
            ScrapingSession session = sessionService.findOrCreateSession(sessionId, request.scraped_by());
            Video video = videoService.ingestVideo(request, session);

            if (video == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to ingest video - missing video_id"));
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "videoId", video.getId(),
                    "platformId", video.getPlatformId(),
                    "sessionId", session.getSessionUuid().toString()
            )));
        } catch (Exception e) {
            log.error("Error ingesting video", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Ingest comment batches
     */
    @PostMapping("/tiktok/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestComments(
            @RequestBody CommentsPayloadDto request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        // 1. PRINT LOGS
        try {
            contentService.processComments(request);
        } catch (Exception e) {
            log.warn("Console logging failed: {}", e.getMessage());
        }

        // 2. SAVE TO DB
        try {
            ScrapingSession session = sessionService.findOrCreateSession(sessionId, request.scraped_by());
            int ingested = commentService.ingestComments(request, session);

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "ingested", ingested,
                    "total", request.comments() != null ? request.comments().size() : 0,
                    "videoId", request.comments() != null && !request.comments().isEmpty()
                            ? request.comments().get(0).video_id() : "unknown"
            )));
        } catch (Exception e) {
            log.error("Error ingesting comments", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Ingest profile visit events
     */
    @PostMapping("/tiktok/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestProfileVisit(
            @RequestBody ProfileVisitDto request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        // 1. PRINT LOGS
        try {
            contentService.processProfileVisit(request);
        } catch (Exception e) {
            log.warn("Console logging failed: {}", e.getMessage());
        }

        // 2. SAVE TO DB
        try {
            Profile profile = profileService.updateFromVisit(request);

            if (profile == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to process profile visit - missing profile_handle"));
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "profileId", profile.getId(),
                    "handle", profile.getPlatformHandle()
            )));
        } catch (Exception e) {
            log.error("Error ingesting profile visit", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Ingest user interactions (likes/saves)
     */
    @PostMapping("/tiktok/interactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestInteractions(
            @RequestBody InteractionPayloadDto request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        // 1. PRINT LOGS
        try {
            contentService.processInteractions(request);
        } catch (Exception e) {
            log.warn("Console logging failed: {}", e.getMessage());
        }

        // 2. SAVE TO DB
        try {
            ScrapingSession session = sessionService.findOrCreateSession(sessionId, request.scraped_by());
            int ingested = behaviorService.ingestInteractions(request, session);

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "ingested", ingested,
                    "total", request.interactions() != null ? request.interactions().size() : 0
            )));
        } catch (Exception e) {
            log.error("Error ingesting interactions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Ingest search events
     */
    @PostMapping("/tiktok/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestSearchEvent(
            @RequestBody SearchEventDto request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        // 1. PRINT LOGS
        try {
            contentService.processSearchEvent(request);
        } catch (Exception e) {
            log.warn("Console logging failed: {}", e.getMessage());
        }

        // 2. SAVE TO DB
        try {
            ScrapingSession session = sessionService.findOrCreateSession(sessionId, request.scraped_by());
            SearchEvent searchEvent = behaviorService.ingestSearchEvent(request, session);

            if (searchEvent == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to process search event - missing query"));
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "searchId", searchEvent.getId(),
                    "query", searchEvent.getQuery()
            )));
        } catch (Exception e) {
            log.error("Error ingesting search event", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Ingest watch time events
     */
    @PostMapping("/tiktok/watchtime")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestWatchTime(
            @RequestBody WatchTimeDto request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        // 1. PRINT LOGS
        try {
            contentService.processWatchTime(request);
        } catch (Exception e) {
            log.warn("Console logging failed: {}", e.getMessage());
        }

        // 2. SAVE TO DB
        try {
            ScrapingSession session = sessionService.findOrCreateSession(sessionId, request.scraped_by());
            WatchTime watchTime = behaviorService.ingestWatchTime(request, session);

            if (watchTime == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to process watch time - missing video_id"));
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "watchTimeId", watchTime.getId(),
                    "durationMs", watchTime.getDurationMs()
            )));
        } catch (Exception e) {
            log.error("Error ingesting watch time", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Server error: " + e.getMessage()));
        }
    }
}