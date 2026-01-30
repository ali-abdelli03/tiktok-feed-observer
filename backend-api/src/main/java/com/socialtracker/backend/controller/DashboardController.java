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

import java.util.Collections;
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
    private final TesterService testerService; // Added

    @GetMapping
    public String dashboard(Model model) {
        DashboardStatsDto stats = analyticsService.getDashboardStats();
        model.addAttribute("stats", stats);

        List<VideoResponseDto> recentVideos = videoService.findRecentVideos(24, 5).stream()
                .map(videoService::toDto)
                .collect(Collectors.toList());
        model.addAttribute("recentVideos", recentVideos);

        List<ProfileResponseDto> topProfiles = profileService.findTopByVideoCount(5).stream()
                .map(profileService::toDto)
                .collect(Collectors.toList());
        model.addAttribute("topProfiles", topProfiles);

        return "dashboard/index";
    }

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

    @GetMapping("/videos/{platformId}")
    public String videoDetail(@PathVariable String platformId, Model model) {
        return videoService.findByPlatformId(platformId)
                .map(video -> {
                    model.addAttribute("video", videoService.toDto(video));

                    List<VideoStatsResponseDto> statsHistory = videoService.getStatsHistory(video.getId()).stream()
                            .map(videoService::toStatsDto)
                            .collect(Collectors.toList());
                    model.addAttribute("statsHistory", statsHistory);

                    List<CommentResponseDto> comments = commentService.findByVideoId(video.getId()).stream()
                            .map(commentService::toDto)
                            .collect(Collectors.toList());
                    model.addAttribute("comments", comments);

                    return "dashboard/video-detail";
                })
                .orElse("redirect:/dashboard/videos");
    }

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

    @GetMapping("/profiles/{handle}")
    public String profileDetail(@PathVariable String handle, Model model) {
        return profileService.findByHandle(handle)
                .map(profile -> {
                    model.addAttribute("profile", profileService.toDto(profile));

                    List<VideoResponseDto> videos = profile.getVideos().stream()
                            .map(videoService::toDto)
                            .collect(Collectors.toList());
                    model.addAttribute("videos", videos);

                    return "dashboard/profile-detail";
                })
                .orElse("redirect:/dashboard/profiles");
    }

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
            comments = commentService.findLikedByAuthor().stream()
                    .limit(100)
                    .map(commentService::toDto)
                    .collect(Collectors.toList());
        }

        model.addAttribute("comments", comments);
        return "dashboard/comments";
    }

    @GetMapping("/testers")
    public String testers(Model model) {
        List<Tester> testers = testerService.findAll();
        model.addAttribute("testers", testers);
        return "dashboard/testers";
    }

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
            } else if ("testers".equals(type)) {
                // Simple search for testers by username
                Tester tester = testerService.findByUsername(q).orElse(null);
                model.addAttribute("results", tester != null ? List.of(tester) : Collections.emptyList());
            }
        }

        return "dashboard/search";
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}