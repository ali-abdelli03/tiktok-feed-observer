package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.response.*;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.service.*;
import com.socialtracker.backend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MVC Controller for the Cyberpunk Dashboard
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final VideoService videoService;
    private final ProfileService profileService;
    private final CommentService commentService;
    private final TesterService testerService;
    private final HashtagRepository hashtagRepository;
    private final MusicRepository musicRepository;
    private final EffectRepository effectRepository;
    private final PlaceRepository placeRepository;
    private final WatchTimeRepository watchTimeRepository;
    private final SearchEventRepository searchEventRepository;
    private final InteractionRepository interactionRepository;
    private final ProfileVisitRepository profileVisitRepository;
    private final VideoRepository videoRepository;
    private final ProfileRepository profileRepository;

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

    // ==================== ADVANCED SEARCH INTERFACE ====================
    
    /**
     * Advanced Search Page with comprehensive filtering
     */
    @GetMapping("/search")
    public String search(
            // Author filters
            @RequestParam(required = false) String author,
            // Video filters
            @RequestParam(required = false) String hashtag,
            @RequestParam(required = false) String keyword,
            // Content type filters
            @RequestParam(defaultValue = "false") boolean excludeAds,
            @RequestParam(defaultValue = "false") boolean excludeAi,
            @RequestParam(defaultValue = "false") boolean verifiedOnly,
            // Time filter
            @RequestParam(required = false) String timeRange,
            // Search type
            @RequestParam(defaultValue = "videos") String type,
            Model model
    ) {
        // Pass all filter values back to template
        model.addAttribute("author", author);
        model.addAttribute("hashtag", hashtag);
        model.addAttribute("keyword", keyword);
        model.addAttribute("excludeAds", excludeAds);
        model.addAttribute("excludeAi", excludeAi);
        model.addAttribute("verifiedOnly", verifiedOnly);
        model.addAttribute("timeRange", timeRange);
        model.addAttribute("type", type);
        
        // Parse time range
        LocalDateTime since = parseTimeRange(timeRange);
        
        // Check if any search is active
        boolean hasSearch = (author != null && !author.isBlank()) 
                         || (hashtag != null && !hashtag.isBlank()) 
                         || (keyword != null && !keyword.isBlank());
        
        if (hasSearch || !"videos".equals(type)) {
            switch (type) {
                case "videos":
                    performVideoSearch(author, hashtag, keyword, excludeAds, excludeAi, verifiedOnly, since, model);
                    break;
                case "creators":
                    performCreatorSearch(author, verifiedOnly, model);
                    break;
                case "music":
                    performMusicSearch(since, model);
                    break;
                case "effects":
                    performEffectsSearch(since, model);
                    break;
                case "places":
                    performPlacesSearch(since, model);
                    break;
            }
        }
        
        // Add aggregated data for related entities if video search was performed
        if ("videos".equals(type) && hasSearch) {
            addRelatedEntityData(model, since);
        }
        
        return "dashboard/search";
    }
    
    private LocalDateTime parseTimeRange(String timeRange) {
        if (timeRange == null) return null;
        
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "1h" -> now.minusHours(1);
            case "24h" -> now.minusHours(24);
            case "7d" -> now.minusDays(7);
            case "30d" -> now.minusDays(30);
            case "90d" -> now.minusDays(90);
            default -> null;
        };
    }
    
    private void performVideoSearch(String author, String hashtag, String keyword,
                                    boolean excludeAds, boolean excludeAi, boolean verifiedOnly,
                                    LocalDateTime since, Model model) {
        // Clean up hashtag (remove # if present)
        String cleanHashtag = hashtag != null && hashtag.startsWith("#") 
                ? hashtag.substring(1) : hashtag;
        
        // Clean up author (remove @ if present)
        String cleanAuthor = author != null && author.startsWith("@") 
                ? author.substring(1) : author;
        
        List<Video> videos = videoRepository.advancedSearch(
                cleanAuthor, 
                cleanHashtag, 
                keyword, 
                excludeAds, 
                excludeAi, 
                verifiedOnly, 
                since
        );
        
        List<VideoResponseDto> results = videos.stream()
                .limit(100)
                .map(videoService::toDto)
                .collect(Collectors.toList());
        
        model.addAttribute("videoResults", results);
        model.addAttribute("videoCount", videos.size());
        
        // Build query description
        StringBuilder queryDesc = new StringBuilder();
        if (cleanAuthor != null && !cleanAuthor.isBlank()) {
            queryDesc.append("@").append(cleanAuthor);
        }
        if (cleanHashtag != null && !cleanHashtag.isBlank()) {
            if (queryDesc.length() > 0) queryDesc.append(" + ");
            queryDesc.append("#").append(cleanHashtag);
        }
        if (keyword != null && !keyword.isBlank()) {
            if (queryDesc.length() > 0) queryDesc.append(" + ");
            queryDesc.append("\"").append(keyword).append("\"");
        }
        model.addAttribute("queryDescription", queryDesc.toString());
    }
    
    private void performCreatorSearch(String search, boolean verifiedOnly, Model model) {
        String cleanSearch = search != null && search.startsWith("@") 
                ? search.substring(1) : search;
        
        List<Profile> profiles = profileRepository.advancedSearch(cleanSearch, verifiedOnly);
        
        List<ProfileResponseDto> results = profiles.stream()
                .limit(50)
                .map(profileService::toDto)
                .collect(Collectors.toList());
        
        model.addAttribute("creatorResults", results);
        model.addAttribute("creatorCount", profiles.size());
    }
    
    private void performMusicSearch(LocalDateTime since, Model model) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(30);
        
        List<Object[]> musicData = musicRepository.findTopMusicSince(
                effectiveSince, PageRequest.of(0, 20));
        
        List<Map<String, Object>> musicResults = musicData.stream()
                .map(row -> {
                    Music music = (Music) row[0];
                    Long count = (Long) row[1];
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", music.getId());
                    item.put("name", music.getName());
                    item.put("platformId", music.getPlatformId());
                    item.put("url", music.getUrl());
                    item.put("usageCount", count);
                    return item;
                })
                .collect(Collectors.toList());
        
        model.addAttribute("musicResults", musicResults);
        model.addAttribute("musicCount", musicResults.size());
    }
    
    private void performEffectsSearch(LocalDateTime since, Model model) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(30);
        
        List<Object[]> effectData = effectRepository.findTopEffectsSince(
                effectiveSince, PageRequest.of(0, 20));
        
        List<Map<String, Object>> effectResults = effectData.stream()
                .map(row -> {
                    Effect effect = (Effect) row[0];
                    Long count = (Long) row[1];
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", effect.getId());
                    item.put("name", effect.getName());
                    item.put("url", effect.getUrl());
                    item.put("usageCount", count);
                    return item;
                })
                .collect(Collectors.toList());
        
        model.addAttribute("effectResults", effectResults);
        model.addAttribute("effectCount", effectResults.size());
    }
    
    private void performPlacesSearch(LocalDateTime since, Model model) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(30);
        
        List<Object[]> placeData = placeRepository.findTopPlacesSince(
                effectiveSince, PageRequest.of(0, 20));
        
        List<Map<String, Object>> placeResults = placeData.stream()
                .map(row -> {
                    Place place = (Place) row[0];
                    Long count = (Long) row[1];
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", place.getId());
                    item.put("name", place.getName());
                    item.put("usageCount", count);
                    return item;
                })
                .collect(Collectors.toList());
        
        model.addAttribute("placeResults", placeResults);
        model.addAttribute("placeCount", placeResults.size());
    }
    
    private void addRelatedEntityData(Model model, LocalDateTime since) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(7);
        
        // Top music from recent videos
        List<Object[]> topMusic = musicRepository.findTopMusicSince(effectiveSince, PageRequest.of(0, 5));
        model.addAttribute("topMusic", topMusic.stream()
                .map(row -> Map.of(
                        "name", ((Music) row[0]).getName(),
                        "count", row[1]
                ))
                .collect(Collectors.toList()));
        
        // Top effects from recent videos
        List<Object[]> topEffects = effectRepository.findTopEffectsSince(effectiveSince, PageRequest.of(0, 5));
        model.addAttribute("topEffects", topEffects.stream()
                .map(row -> Map.of(
                        "name", ((Effect) row[0]).getName(),
                        "count", row[1]
                ))
                .collect(Collectors.toList()));
        
        // Top places from recent videos
        List<Object[]> topPlaces = placeRepository.findTopPlacesSince(effectiveSince, PageRequest.of(0, 5));
        model.addAttribute("topPlaces", topPlaces.stream()
                .map(row -> Map.of(
                        "name", ((Place) row[0]).getName(),
                        "count", row[1]
                ))
                .collect(Collectors.toList()));
    }

    // ==================== NEW ANALYTICS PAGES ====================

    /**
     * Behavior Intelligence Page - Watch time, searches, interactions
     */
    @GetMapping("/behavior")
    public String behavior(Model model) {
        // Watch time stats
        long totalWatchTimeMs = watchTimeRepository.findAll().stream()
                .mapToLong(WatchTime::getDurationMs)
                .sum();
        model.addAttribute("totalWatchTimeFormatted", formatDuration(totalWatchTimeMs));
        
        double avgWatchTimeMs = watchTimeRepository.count() > 0 
                ? (double) totalWatchTimeMs / watchTimeRepository.count() 
                : 0;
        model.addAttribute("avgWatchTimeFormatted", formatDuration((long) avgWatchTimeMs));
        
        // Search and visit counts
        model.addAttribute("totalSearches", searchEventRepository.count());
        model.addAttribute("totalProfileVisits", profileVisitRepository.count());
        
        // Interaction counts by type
        long likesCount = interactionRepository.countActiveLikes();
        long savesCount = interactionRepository.countActiveSaves();
        long sharesCount = interactionRepository.count() - likesCount - savesCount;
        model.addAttribute("likesCount", likesCount);
        model.addAttribute("savesCount", savesCount);
        model.addAttribute("sharesCount", Math.max(0, sharesCount));
        
        // Watch time distribution (buckets: <5s, 5-15s, 15-30s, 30-60s, >60s)
        List<WatchTime> allWatchTimes = watchTimeRepository.findAll();
        int[] distribution = new int[5];
        for (WatchTime wt : allWatchTimes) {
            long sec = wt.getDurationMs() / 1000;
            if (sec < 5) distribution[0]++;
            else if (sec < 15) distribution[1]++;
            else if (sec < 30) distribution[2]++;
            else if (sec < 60) distribution[3]++;
            else distribution[4]++;
        }
        model.addAttribute("watchTimeDistribution", Arrays.stream(distribution).boxed().collect(Collectors.toList()));
        
        // Top searches
        List<Object[]> topSearchData = searchEventRepository.findTopSearchQueries(PageRequest.of(0, 10));
        List<Map<String, Object>> topSearches = topSearchData.stream()
                .map(arr -> Map.of("query", arr[0], "count", arr[1]))
                .collect(Collectors.toList());
        model.addAttribute("topSearches", topSearches);
        
        // Top watched videos
        List<Object[]> topWatchedData = watchTimeRepository.findTopWatchedVideos();
        List<Map<String, Object>> topWatched = topWatchedData.stream()
                .limit(10)
                .map(arr -> {
                    Long videoId = (Long) arr[0];
                    Long totalTime = (Long) arr[1];
                    return videoService.findById(videoId)
                            .map(v -> Map.<String, Object>of(
                                    "platformId", v.getPlatformId(),
                                    "watchTimeFormatted", formatDuration(totalTime),
                                    "viewCount", watchTimeRepository.findByVideoId(videoId).size()
                            ))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        model.addAttribute("topWatched", topWatched);
        
        // Recent interactions
        List<Map<String, Object>> recentInteractions = interactionRepository.findAll(PageRequest.of(0, 20)).stream()
                .map(i -> Map.<String, Object>of(
                        "type", i.getInteractionType(),
                        "videoPlatformId", i.getVideo() != null ? i.getVideo().getPlatformId() : "unknown",
                        "action", i.getAction(),
                        "active", i.getIsActive(),
                        "performedAt", i.getPerformedAt()
                ))
                .collect(Collectors.toList());
        model.addAttribute("recentInteractions", recentInteractions);
        
        return "dashboard/behavior";
    }

    /**
     * Hashtag Analytics Page
     */
    @GetMapping("/hashtags")
    public String hashtags(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);
        
        List<Hashtag> allHashtags;
        if (q != null && !q.isBlank()) {
            allHashtags = hashtagRepository.searchByTag(q);
        } else {
            allHashtags = hashtagRepository.findTopHashtags(PageRequest.of(0, 100));
        }
        
        long totalUsage = allHashtags.stream().mapToLong(Hashtag::getUsageCount).sum();
        
        // Add percentage to each hashtag
        List<Map<String, Object>> hashtagsWithPercent = allHashtags.stream()
                .map(h -> {
                    double pct = totalUsage > 0 ? (h.getUsageCount() * 100.0 / totalUsage) : 0;
                    return Map.<String, Object>of(
                            "tag", h.getTag(),
                            "usageCount", h.getUsageCount(),
                            "percentage", Math.round(pct)
                    );
                })
                .collect(Collectors.toList());
        
        model.addAttribute("hashtags", hashtagsWithPercent);
        model.addAttribute("totalHashtags", hashtagRepository.count());
        model.addAttribute("totalUsage", totalUsage);
        
        // Calculate average tags per video
        long totalVideos = videoService.count();
        double avgTagsPerVideo = totalVideos > 0 ? (double) totalUsage / totalVideos : 0;
        model.addAttribute("avgTagsPerVideo", String.format("%.1f", avgTagsPerVideo));
        
        // Top tag
        String topTag = allHashtags.isEmpty() ? null : allHashtags.get(0).getTag();
        model.addAttribute("topTag", topTag);
        
        // Top 10 for chart
        List<Map<String, Object>> top10 = hashtagsWithPercent.stream().limit(10).collect(Collectors.toList());
        model.addAttribute("topHashtagsJson", top10);
        
        return "dashboard/hashtags";
    }

    /**
     * Music/Audio Analytics Page
     */
    @GetMapping("/music")
    public String music(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);
        
        List<Music> musicList;
        if (q != null && !q.isBlank()) {
            musicList = musicRepository.searchByName(q);
        } else {
            musicList = musicRepository.findAll(PageRequest.of(0, 50)).getContent();
        }
        
        model.addAttribute("musicList", musicList);
        model.addAttribute("totalMusic", musicRepository.count());
        model.addAttribute("totalEffects", effectRepository.count());
        model.addAttribute("totalPlaces", placeRepository.count());
        
        // Recent effects
        List<Effect> recentEffects = effectRepository.findAll(PageRequest.of(0, 5)).getContent();
        model.addAttribute("recentEffects", recentEffects);
        
        // Recent places
        List<Place> recentPlaces = placeRepository.findAll(PageRequest.of(0, 5)).getContent();
        model.addAttribute("recentPlaces", recentPlaces);
        
        return "dashboard/music";
    }

    // ==================== UTILITY METHODS ====================

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
    
    // ==================== REST API ENDPOINTS ====================
    
    /**
     * API endpoint for real-time timeline data
     * @param range Time range: "24h", "7d", or "30d"
     * @return JSON with time-series data for charts
     */
    @GetMapping("/api/timeline")
    @ResponseBody
    public TimeSeriesDataDto getTimelineData(@RequestParam(defaultValue = "24h") String range) {
        return analyticsService.getTimelineData(range);
    }
}