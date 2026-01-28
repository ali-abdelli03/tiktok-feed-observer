package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.CommentsPayloadDto;
import com.socialtracker.backend.dto.ContentDto;
import com.socialtracker.backend.dto.InteractionPayloadDto;
import com.socialtracker.backend.dto.ProfileVisitDto;
import com.socialtracker.backend.dto.SearchEventDto;
import com.socialtracker.backend.dto.WatchTimeDto;
import com.socialtracker.backend.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows Chrome Extension access
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * Endpoint for video/content data from the TikTok scraper
     */
    @PostMapping("/tiktok")
    public ResponseEntity<Map<String, Object>> uploadContent(
            @RequestBody ContentDto request
    ) {
        try {
            contentService.processContent(request);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for comment batches from the TikTok scraper
     */
    @PostMapping("/tiktok/comments")
    public ResponseEntity<Map<String, Object>> uploadComments(
            @RequestBody CommentsPayloadDto request
    ) {
        try {
            contentService.processComments(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", request.comments() != null ? request.comments().size() : 0
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for search events from the TikTok scraper
     */
    @PostMapping("/tiktok/search")
    public ResponseEntity<Map<String, Object>> uploadSearchEvent(
            @RequestBody SearchEventDto request
    ) {
        try {
            contentService.processSearchEvent(request);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for user interactions (likes/saves) from the TikTok scraper
     */
    @PostMapping("/tiktok/interactions")
    public ResponseEntity<Map<String, Object>> uploadInteractions(
            @RequestBody InteractionPayloadDto request
    ) {
        try {
            contentService.processInteractions(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", request.interactions() != null ? request.interactions().size() : 0
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for profile visit events from the TikTok scraper
     */
    @PostMapping("/tiktok/profile")
    public ResponseEntity<Map<String, Object>> uploadProfileVisit(
            @RequestBody ProfileVisitDto request
    ) {
        try {
            contentService.processProfileVisit(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "profile", request.profile_handle() != null ? request.profile_handle() : "unknown"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for watch time events from the TikTok scraper
     */
    @PostMapping("/tiktok/watchtime")
    public ResponseEntity<Map<String, Object>> uploadWatchTime(
            @RequestBody WatchTimeDto request
    ) {
        try {
            contentService.processWatchTime(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "video_id", request.video_id() != null ? request.video_id() : "unknown"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }
}

