package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.CommentDto;
import com.socialtracker.backend.dto.CommentsPayloadDto;
import com.socialtracker.backend.dto.response.CommentResponseDto;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.repository.CommentMentionRepository;
import com.socialtracker.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing comments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final VideoService videoService;
    private final SessionService sessionService;
    
    /**
     * Ingest comments batch from scraper
     */
    @Transactional
    public int ingestComments(CommentsPayloadDto payload, ScrapingSession session) {
        List<CommentDto> comments = payload.comments();
        if (comments == null || comments.isEmpty()) {
            return 0;
        }
        
        // Extract video ID from first comment
        String videoPlatformId = comments.get(0).video_id();
        if (videoPlatformId == null || videoPlatformId.isBlank()) {
            log.warn("Received comments without video ID");
            return 0;
        }

        // Find or get video reference
        Video video = videoService.findOrCreateSkeleton(videoPlatformId);
        
        int ingested = 0;
        for (CommentDto dto : comments) {
            if (ingestSingleComment(dto, video)) {
                ingested++;
            }
        }
        
        // Update session counter
        if (session != null && ingested > 0) {
            for (int i = 0; i < ingested; i++) {
                sessionService.incrementCommentCount(session);
            }
        }
        
        log.info("Ingested {} comments for video: {}", ingested, videoPlatformId);
        return ingested;
    }
    
    private boolean ingestSingleComment(CommentDto dto, Video video) {
        String commentHash = dto.id();
        if (commentHash == null || commentHash.isBlank()) {
            // Generate hash from content if not provided
            commentHash = generateCommentHash(video.getPlatformId(), dto.author_handle(), dto.text_comment());
        }
        
        // Skip if already exists
        if (commentRepository.existsByCommentHash(commentHash)) {
            return false;
        }
        
        Comment comment = Comment.builder()
                .commentHash(commentHash)
                .video(video)
                .authorHandle(dto.author_handle())
                .authorName(dto.author_name())
                .textContent(dto.text_comment())
                .allText(dto.all_text())
                .imageUrl(dto.image_url())
                .likes(dto.likes() != null ? dto.likes() : 0)
                .likedByAuthor(Boolean.TRUE.equals(dto.liked_by_author()))
                .build();
        
        comment = commentRepository.save(comment);
        
        // Process mentions
        processMentions(comment, dto.mentions());
        
        return true;
    }
    
    private void processMentions(Comment comment, List<Map<String, Object>> mentions) {
        if (mentions == null || mentions.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> mentionData : mentions) {
            String displayName = (String) mentionData.get("displayName");
            String encodedUrl = (String) mentionData.get("encodedUrl");
            
            if (displayName != null && !displayName.isBlank()) {
                CommentMention mention = CommentMention.builder()
                        .comment(comment)
                        .displayName(displayName)
                        .encodedUrl(encodedUrl)
                        .build();
                commentMentionRepository.save(mention);
            }
        }
    }
    
    private String generateCommentHash(String videoId, String author, String text) {
        String input = videoId + ":" + author + ":" + (text != null ? text.substring(0, Math.min(50, text.length())) : "");
        return "cmt:" + Math.abs(input.hashCode());
    }
    
    // ========== QUERY METHODS ==========
    
    public List<Comment> findByVideoId(Long videoId) {
        return commentRepository.findByVideoId(videoId);
    }
    
    public Page<Comment> findByVideoIdPaged(Long videoId, Pageable pageable) {
        return commentRepository.findByVideoIdOrderByCapturedAtDesc(videoId, pageable);
    }
    
    public List<Comment> findByVideoPlatformId(String platformId) {
        return commentRepository.findByVideoPlatformId(platformId);
    }
    
    public List<Comment> findLikedByAuthor() {
        return commentRepository.findLikedByAuthor();
    }
    
    public List<Comment> searchByContent(String keyword) {
        return commentRepository.searchByContent(keyword);
    }
    
    public long count() {
        return commentRepository.count();
    }
    
    public long countByVideoId(Long videoId) {
        return commentRepository.countByVideoId(videoId);
    }
    
    // ========== DTO CONVERSION ==========
    
    public CommentResponseDto toDto(Comment comment) {
        List<CommentResponseDto.MentionDto> mentions = comment.getMentions().stream()
                .map(m -> CommentResponseDto.MentionDto.builder()
                        .displayName(m.getDisplayName())
                        .encodedUrl(m.getEncodedUrl())
                        .build())
                .collect(Collectors.toList());
        
        return CommentResponseDto.builder()
                .id(comment.getId())
                .commentHash(comment.getCommentHash())
                .videoPlatformId(comment.getVideo().getPlatformId())
                .authorHandle(comment.getAuthorHandle())
                .authorName(comment.getAuthorName())
                .textContent(comment.getTextContent())
                .imageUrl(comment.getImageUrl())
                .likes(comment.getLikes())
                .likedByAuthor(comment.getLikedByAuthor())
                .mentions(mentions)
                .capturedAt(comment.getCapturedAt())
                .build();
    }
}
