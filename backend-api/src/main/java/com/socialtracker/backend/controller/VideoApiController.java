package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.response.*;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for querying video data
 */
@RestController
@RequestMapping("/api/v2/videos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VideoApiController {
    
    private final VideoService videoService;
    private final CommentService commentService;
    
    /**
     * Get all videos with pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VideoResponseDto>>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Video> videos = videoService.findAll(PageRequest.of(page, size));
        Page<VideoResponseDto> dtos = videos.map(videoService::toDto);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Get a single video by platform ID
     */
    @GetMapping("/{platformId}")
    public ResponseEntity<ApiResponse<VideoResponseDto>> getVideo(@PathVariable String platformId) {
        return videoService.findByPlatformId(platformId)
                .map(video -> ResponseEntity.ok(ApiResponse.success(videoService.toDto(video))))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get video stats history
     */
    @GetMapping("/{platformId}/stats")
    public ResponseEntity<ApiResponse<List<VideoStatsResponseDto>>> getVideoStats(@PathVariable String platformId) {
        return videoService.findByPlatformId(platformId)
                .map(video -> {
                    List<VideoStats> stats = videoService.getStatsHistory(video.getId());
                    List<VideoStatsResponseDto> dtos = stats.stream()
                            .map(videoService::toStatsDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(ApiResponse.success(dtos));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get comments for a video
     */
    @GetMapping("/{platformId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> getVideoComments(
            @PathVariable String platformId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return videoService.findByPlatformId(platformId)
                .map(video -> {
                    Page<Comment> comments = commentService.findByVideoIdPaged(
                            video.getId(), 
                            PageRequest.of(page, size)
                    );
                    List<CommentResponseDto> dtos = comments.stream()
                            .map(commentService::toDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(ApiResponse.success(dtos));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get top videos by likes
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> getTopVideos(
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<Video> videos = videoService.findTopByLikes(PageRequest.of(0, limit));
        List<VideoResponseDto> dtos = videos.stream()
                .map(videoService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Get recent videos
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> getRecentVideos(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<Video> videos = videoService.findRecentVideos(hours, limit);
        List<VideoResponseDto> dtos = videos.stream()
                .map(videoService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Search videos by hashtag
     */
    @GetMapping("/hashtag/{tag}")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> getVideosByHashtag(@PathVariable String tag) {
        List<Video> videos = videoService.findByHashtag(tag);
        List<VideoResponseDto> dtos = videos.stream()
                .map(videoService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Search videos by description keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> searchVideos(@RequestParam String q) {
        List<Video> videos = videoService.searchByDescription(q);
        List<VideoResponseDto> dtos = videos.stream()
                .map(videoService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}
