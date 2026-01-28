package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.CommentDto;
import com.socialtracker.backend.dto.CommentsPayloadDto;
import com.socialtracker.backend.dto.ContentDto;
import com.socialtracker.backend.dto.InteractionDto;
import com.socialtracker.backend.dto.InteractionPayloadDto;
import com.socialtracker.backend.dto.ProfileVisitDto;
import com.socialtracker.backend.dto.SearchEventDto;
import com.socialtracker.backend.dto.WatchTimeDto;
import org.springframework.stereotype.Service;

@Service
public class ContentService {

    // Lock object to prevent interleaved console output
    private static final Object PRINT_LOCK = new Object();

    /**
     * Process video/content data from the TikTok scraper
     */
    public void processContent(ContentDto request) {
        synchronized (PRINT_LOCK) {
            System.out.println("================== VIDEO ==================");
            System.out.println("Scraped By: " + request.scraped_by());
            System.out.println("Context: " + (request.context_type() != null ? request.context_type() : "FOR_YOU"));
            System.out.println("Author: " + request.author_handle());
            System.out.println("Is Verified: " + request.is_verified());
            System.out.println("Is Ad: " + request.is_ad());
            System.out.println("Is Live: " + request.is_live());
            System.out.println("Is AI: " + request.is_ai());
            System.out.println("Description: " + request.description());
            System.out.println("Hashtags: " + request.hashtags());
            System.out.println("Mentions: " + request.mentions());
            System.out.println("Place: " + request.place());
            System.out.println("Place Id: " + request.place_id());
            System.out.println("--- Stats (parsed → raw) ---");
            System.out.println("  Likes: " + request.stat_likes() + " ← " + request.stat_likes_raw());
            System.out.println("  Comments: " + request.stat_comments() + " ← " + request.stat_comments_raw());
            System.out.println("  Saved: " + request.stat_saved() + " ← " + request.stat_saved_raw());
            System.out.println("  Shared: " + request.stat_shared() + " ← " + request.stat_shared_raw());
            System.out.println("----------------------------");
            System.out.println("Timestamp: " + request.timestamp());
            System.out.println("Music Name: " + request.music_name());
            System.out.println("Music Id: " + request.music_id());
            System.out.println("Music Url: " + request.music_url());
            System.out.println("Effect Name: " + request.effect_name());
            System.out.println("Effect Id: " + request.effect_id());
            System.out.println("Effect Url: " + request.effect_url());
            System.out.println("Video Id: " + request.video_id());
            System.out.println("Video Url: " + request.video_url());
            System.out.println("Session Sequence: " + request.session_sequence());
            System.out.println("============================================");
            System.out.println();
        }
    }
    
    /**
     * Format watch time in a human-readable format
     */
    private String formatWatchTime(Long ms) {
        if (ms == null || ms == 0) return "0s";
        
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds (%dms)", minutes, seconds, ms);
        }
        return String.format("%ds (%dms)", seconds, ms);
    }

    /**
     * Process comment batches from the TikTok scraper
     */
    public void processComments(CommentsPayloadDto request) {
        synchronized (PRINT_LOCK) {
            System.out.println("================ COMMENTS ==================");
            System.out.println("Scraped By: " + request.scraped_by());
            System.out.println("Batch Timestamp: " + request.timestamp());
            System.out.println("Comment Count: " + (request.comments() != null ? request.comments().size() : 0));
            System.out.println("--------------------------------------------");

            if (request.comments() != null) {
                for (CommentDto comment : request.comments()) {
                    System.out.println("  [Comment ID: " + comment.id() + "]");
                    System.out.println("    Video ID: " + comment.video_id());
                    System.out.println("    Author: @" + comment.author_handle());
                    System.out.println("    Name: " + (comment.author_name() != null ? comment.author_name() : "N/A"));
                    System.out.println("    Text Comment: " + (comment.text_comment() != null ? truncate(comment.text_comment(), 200) : "N/A"));
                    System.out.println("    All Text: " + (comment.all_text() != null ? truncate(comment.all_text(), 200) : "N/A"));
                    if (comment.image_url() != null) {
                        System.out.println("    Image URL: " + comment.image_url());
                    }
                    if (comment.likes() != null) {
                        System.out.println("    Likes: " + comment.likes());
                    }
                    if (Boolean.TRUE.equals(comment.liked_by_author())) {
                        System.out.println("    ❤️ LIKED BY AUTHOR");
                    }
                    if (comment.mentions() != null && !comment.mentions().isEmpty()) {
                        System.out.println("    Mentions: " + comment.mentions().size() + " user(s)");
                        for (var mention : comment.mentions()) {
                            String displayName = mention.get("displayName") != null ? mention.get("displayName").toString() : "N/A";
                            String encodedUrl = mention.get("encodedUrl") != null ? mention.get("encodedUrl").toString() : "N/A";
                            boolean isEncoded = Boolean.TRUE.equals(mention.get("isEncoded"));
                            System.out.println("      → @" + displayName);
                            System.out.println("         URL: " + encodedUrl + (isEncoded ? " (encoded)" : ""));
                        }
                    }
                    System.out.println();
                }
            }
            System.out.println("============================================");
            System.out.println();
        }
    }

    /**
     * Process search events from the TikTok scraper
     */
    public void processSearchEvent(SearchEventDto request) {
        synchronized (PRINT_LOCK) {
            System.out.println("================ SEARCH ====================");
            System.out.println("Scraped By: " + request.scraped_by());
            System.out.println("Type: " + request.type());
            System.out.println("Query: \"" + request.query() + "\"");
            System.out.println("Timestamp: " + request.timestamp());
            System.out.println("URL: " + request.url());
            System.out.println("============================================");
            System.out.println();
        }
    }

    /**
     * Process user interactions (likes/saves) from the TikTok scraper
     */
    public void processInteractions(InteractionPayloadDto request) {
        synchronized (PRINT_LOCK) {
            System.out.println("============== INTERACTIONS ================");
            System.out.println("Scraped By: " + request.scraped_by());
            System.out.println("Batch Timestamp: " + request.timestamp());
            System.out.println("Interaction Count: " + (request.interactions() != null ? request.interactions().size() : 0));
            System.out.println("--------------------------------------------");

            if (request.interactions() != null) {
                for (InteractionDto interaction : request.interactions()) {
                    String icon = getInteractionIcon(interaction.type(), interaction.is_active());
                    String action = interaction.action() != null ? interaction.action() : "unknown";
                    String context = interaction.context_type() != null ? interaction.context_type() : "N/A";
                    System.out.println("  " + icon + " " + interaction.type().toUpperCase() + " [" + action + "]");
                    System.out.println("    Video ID: " + interaction.video_id());
                    System.out.println("    State: " + (Boolean.TRUE.equals(interaction.is_active()) ? "ACTIVE" : "INACTIVE"));
                    System.out.println("    Context: " + context);
                    System.out.println("    Timestamp: " + interaction.timestamp());
                    System.out.println();
                }
            }
            System.out.println("============================================");
            System.out.println();
        }
    }

    /**
     * Get emoji icon for interaction display
     */
    private String getInteractionIcon(String type, Boolean isActive) {
        if (type == null) return "❓";
        
        return switch (type.toLowerCase()) {
            case "like" -> isActive ? "❤️" : "🤍";
            case "save" -> isActive ? "⭐" : "☆";
            default -> "📌";
        };
    }

    /**
     * Process profile visit events from the TikTok scraper
     */
    public void processProfileVisit(ProfileVisitDto request) {
        synchronized (PRINT_LOCK) {
            System.out.println("============= PROFILE VISIT ================");
            System.out.println("Scraped By: " + request.scraped_by());
            System.out.println("Timestamp: " + request.timestamp());
            System.out.println("--------------------------------------------");
            System.out.println("Profile: @" + request.profile_handle());
            System.out.println("Display Name: " + (request.display_name() != null ? request.display_name() : "N/A"));
            System.out.println("URL: " + request.profile_url());
            
            // Profile stats
            System.out.println("--- Profile Stats ---");
            System.out.println("  Followers: " + (request.follower_count() != null ? request.follower_count() : "N/A"));
            System.out.println("  Following: " + (request.following_count() != null ? request.following_count() : "N/A"));
            System.out.println("  Likes: " + (request.likes_count() != null ? request.likes_count() : "N/A"));
            System.out.println("  Verified: " + (Boolean.TRUE.equals(request.is_verified()) ? "✓ YES" : "NO"));
            
            // Bio and external link
            if (request.bio() != null && !request.bio().isEmpty()) {
                System.out.println("Bio: " + request.bio());
            }
            if (request.profile_link() != null && !request.profile_link().isEmpty()) {
                System.out.println("External Link: " + request.profile_link());
            }
            
            // Context
            System.out.println("--- Context ---");
            System.out.println("  Context Type: " + (request.context_type() != null ? request.context_type() : "N/A"));
            System.out.println("  Referrer: " + (request.referrer_url() != null ? request.referrer_url() : "N/A"));
            System.out.println("  Visited At: " + request.visited_at());
            System.out.println("============================================");
            System.out.println();
        }
    }

    /**
     * Process watch time events from the TikTok scraper
     */
    public void processWatchTime(WatchTimeDto request) {
        synchronized (PRINT_LOCK) {
            System.out.println("============== WATCH TIME ==================");
            System.out.println("Scraped By: " + request.scraped_by());
            System.out.println("Timestamp: " + request.timestamp());
            System.out.println("--------------------------------------------");
            System.out.println("Video ID: " + request.video_id());
            System.out.println("Author: @" + request.author_handle());
            System.out.println("Duration: " + formatWatchTime(request.watch_duration_ms()));
            System.out.println("URL: " + request.video_url());
            System.out.println("============================================");
            System.out.println();
        }
    }

    /**
     * Helper to truncate long strings for logging
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
