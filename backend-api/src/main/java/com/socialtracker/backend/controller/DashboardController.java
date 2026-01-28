package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.response.*;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MVC Controller for the Bootstrap dashboard
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final AnalyticsService analyticsService;
    private final VideoService videoService;
    private final ProfileService profileService;
    private final CommentService commentService;
    
    /**
     * Main dashboard page with statistics overview
     */
    @GetMapping
    public String dashboard(Model model) {
        DashboardStatsDto stats = analyticsService.getDashboardStats();
        model.addAttribute("stats", stats);
        
        // Recent videos
        List<VideoResponseDto> recentVideos = videoService.findRecentVideos(24, 5).stream()
                .map(videoService::toDto)
                .collect(Collectors.toList());
        model.addAttribute("recentVideos", recentVideos);
        
        // Top profiles
        List<ProfileResponseDto> topProfiles = profileService.findTopByVideoCount(5).stream()
                .map(profileService::toDto)
                .collect(Collectors.toList());
        model.addAttribute("topProfiles", topProfiles);
        
        return "dashboard/index";
    }
    
    /**
     * Videos list page with pagination
     */
    @GetMapping("/videos")
    public String videos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Page<Video> videoPage = videoService.findAll(PageRequest.of(page, size));
        Page<VideoResponseDto> videos = videoPage.map(videoService::toDto);
        
        model.addAttribute("videos", videos);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", videos.getTotalPages());
        
        return "dashboard/videos";
    }
    
    /**
     * Single video detail page
     */
    @GetMapping("/videos/{platformId}")
    public String videoDetail(@PathVariable String platformId, Model model) {
        return videoService.findByPlatformId(platformId)
                .map(video -> {
                    model.addAttribute("video", videoService.toDto(video));
                    
                    // Stats history
                    List<VideoStatsResponseDto> statsHistory = videoService.getStatsHistory(video.getId()).stream()
                            .map(videoService::toStatsDto)
                            .collect(Collectors.toList());
                    model.addAttribute("statsHistory", statsHistory);
                    
                    // Comments
                    List<CommentResponseDto> comments = commentService.findByVideoId(video.getId()).stream()
                            .map(commentService::toDto)
                            .collect(Collectors.toList());
                    model.addAttribute("comments", comments);
                    
                    return "dashboard/video-detail";
                })
                .orElse("redirect:/dashboard/videos");
    }
    
    /**
     * Profiles list page
     */
    @GetMapping("/profiles")
    public String profiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Page<Profile> profilePage = profileService.findAll(PageRequest.of(page, size));
        Page<ProfileResponseDto> profiles = profilePage.map(profileService::toDto);
        
        model.addAttribute("profiles", profiles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", profiles.getTotalPages());
        
        return "dashboard/profiles";
    }
    
    /**
     * Single profile detail page
     */
    @GetMapping("/profiles/{handle}")
    public String profileDetail(@PathVariable String handle, Model model) {
        return profileService.findByHandle(handle)
                .map(profile -> {
                    model.addAttribute("profile", profileService.toDto(profile));
                    
                    // Profile's videos
                    List<VideoResponseDto> videos = profile.getVideos().stream()
                            .map(videoService::toDto)
                            .collect(Collectors.toList());
                    model.addAttribute("videos", videos);
                    
                    return "dashboard/profile-detail";
                })
                .orElse("redirect:/dashboard/profiles");
    }
    
    /**
     * Comments page
     */
    @GetMapping("/comments")
    public String comments(
            @RequestParam(required = false) String videoId,
            Model model
    ) {
        List<CommentResponseDto> comments;
        
        if (videoId != null && !videoId.isBlank()) {
            comments = commentService.findByVideoPlatformId(videoId).stream()
                    .map(commentService::toDto)
                    .collect(Collectors.toList());
            model.addAttribute("videoId", videoId);
        } else {
            // Show recent comments (first 100)
            comments = commentService.findLikedByAuthor().stream()
                    .limit(100)
                    .map(commentService::toDto)
                    .collect(Collectors.toList());
        }
        
        model.addAttribute("comments", comments);
        return "dashboard/comments";
    }
    
    /**
     * Search page
     */
    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "videos") String type,
            Model model
    ) {
        model.addAttribute("query", q);
        model.addAttribute("type", type);
        
        if (q != null && !q.isBlank()) {
            if ("videos".equals(type)) {
                List<VideoResponseDto> results = videoService.searchByDescription(q).stream()
                        .map(videoService::toDto)
                        .collect(Collectors.toList());
                model.addAttribute("results", results);
            } else if ("profiles".equals(type)) {
                List<ProfileResponseDto> results = profileService.searchProfiles(q).stream()
                        .map(profileService::toDto)
                        .collect(Collectors.toList());
                model.addAttribute("results", results);
            }
        }
        
        return "dashboard/search";
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
