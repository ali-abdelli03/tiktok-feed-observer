package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.InteractionDto;
import com.socialtracker.backend.dto.InteractionPayloadDto;
import com.socialtracker.backend.dto.SearchEventDto;
import com.socialtracker.backend.dto.WatchTimeDto;
import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user behavior data (interactions, watch time, searches)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorService {
    
    private final InteractionRepository interactionRepository;
    private final WatchTimeRepository watchTimeRepository;
    private final SearchEventRepository searchEventRepository;
    private final VideoService videoService;
    private final TesterService testerService;
    private final SessionService sessionService;
    
    // ========== INTERACTIONS ==========
    
    /**
     * Ingest interactions batch from scraper
     */
    @Transactional
    public int ingestInteractions(InteractionPayloadDto payload, ScrapingSession session) {
        List<InteractionDto> interactions = payload.interactions();
        if (interactions == null || interactions.isEmpty()) {
            return 0;
        }
        
        int ingested = 0;
        for (InteractionDto dto : interactions) {
            if (ingestSingleInteraction(dto, session)) {
                ingested++;
            }
        }
        
        log.info("Ingested {} interactions", ingested);
        return ingested;
    }
    
    private boolean ingestSingleInteraction(InteractionDto dto, ScrapingSession session) {
        String videoPlatformId = dto.video_id();
        if (videoPlatformId == null || videoPlatformId.isBlank()) {
            return false;
        }
        
        //Optional<Video> videoOpt = videoService.findByPlatformId(videoPlatformId);
        Video video = videoService.findOrCreateSkeleton(videoPlatformId);
        
        Interaction interaction = Interaction.builder()
                .session(session)
                .video(video)
                .interactionType(dto.type())
                .action(dto.action())
                .isActive(Boolean.TRUE.equals(dto.is_active()))
                .contextType(dto.context_type())
                .build();
        
        interactionRepository.save(interaction);
        
        if (session != null) {
            sessionService.incrementInteractionCount(session);
        }
        
        return true;
    }
    
    public List<Interaction> findInteractionsByVideoId(Long videoId) {
        return interactionRepository.findByVideoId(videoId);
    }
    
    public long countActiveLikes() {
        return interactionRepository.countActiveLikes();
    }
    
    public long countActiveSaves() {
        return interactionRepository.countActiveSaves();
    }
    
    // ========== WATCH TIME ==========
    
    /**
     * Ingest watch time event from scraper
     */
    @Transactional
    public WatchTime ingestWatchTime(WatchTimeDto dto, ScrapingSession session) {
        String videoPlatformId = dto.video_id();
        if (videoPlatformId == null || videoPlatformId.isBlank()) {
            return null;
        }
        Video video = videoService.findOrCreateSkeleton(videoPlatformId);
        
        WatchTime watchTime = WatchTime.builder()
                .session(session)
                .video(video)
                .durationMs(dto.watch_duration_ms() != null ? dto.watch_duration_ms() : 0L)
                .build();
        
        watchTime = watchTimeRepository.save(watchTime);
        log.debug("Recorded watch time: {}ms for video: {}", dto.watch_duration_ms(), videoPlatformId);
        
        return watchTime;
    }
    
    public Long getTotalWatchTimeForVideo(Long videoId) {
        Long total = watchTimeRepository.getTotalWatchTimeForVideo(videoId);
        return total != null ? total : 0L;
    }
    
    public Double getAverageWatchTimeForVideo(Long videoId) {
        return watchTimeRepository.getAverageWatchTimeForVideo(videoId);
    }
    
    // ========== SEARCH EVENTS ==========
    
    /**
     * Ingest search event from scraper
     */
    @Transactional
    public SearchEvent ingestSearchEvent(SearchEventDto dto, ScrapingSession session) {
        if (dto.query() == null || dto.query().isBlank()) {
            return null;
        }
        
        Tester tester = testerService.findOrCreateTester(dto.scraped_by());
        
        SearchEvent searchEvent = SearchEvent.builder()
                .session(session)
                .tester(tester)
                .query(dto.query())
                .url(dto.url())
                .build();
        
        searchEvent = searchEventRepository.save(searchEvent);
        log.info("Recorded search: '{}' by {}", dto.query(), dto.scraped_by());
        
        return searchEvent;
    }
    
    public List<SearchEvent> findSearchesByTester(String username) {
        return searchEventRepository.findByTesterUsername(username);
    }
    
    public List<Object[]> findTopSearchQueries(int limit) {
        return searchEventRepository.findTopSearchQueries(org.springframework.data.domain.PageRequest.of(0, limit));
    }
}
