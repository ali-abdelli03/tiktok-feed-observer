package com.socialtracker.backend.service;

import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing media metadata (music, effects, hashtags, places)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {
    
    private final MusicRepository musicRepository;
    private final EffectRepository effectRepository;
    private final HashtagRepository hashtagRepository;
    private final PlaceRepository placeRepository;
    
    // ========== MUSIC ==========
    
    @Transactional
    public Music findOrCreateMusic(String platformId, String name, String url) {
        if ((platformId == null || platformId.isBlank()) && 
            (name == null || name.isBlank())) {
            return null;
        }
        
        // Try to find by platform ID first
        if (platformId != null && !platformId.isBlank()) {
            Optional<Music> existing = musicRepository.findByPlatformId(platformId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        
        // Create new music entry
        Music music = Music.builder()
                .platformId(platformId)
                .name(name)
                .url(url)
                .build();
        
        log.debug("Created new music: {}", name);
        return musicRepository.save(music);
    }
    
    public Optional<Music> findMusicByPlatformId(String platformId) {
        return musicRepository.findByPlatformId(platformId);
    }
    
    // ========== EFFECT ==========
    
    @Transactional
    public Effect findOrCreateEffect(String platformId, String name, String url) {
        if ((platformId == null || platformId.isBlank()) && 
            (name == null || name.isBlank())) {
            return null;
        }
        
        if (platformId != null && !platformId.isBlank()) {
            Optional<Effect> existing = effectRepository.findByPlatformId(platformId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        
        Effect effect = Effect.builder()
                .platformId(platformId)
                .name(name)
                .url(url)
                .build();
        
        log.debug("Created new effect: {}", name);
        return effectRepository.save(effect);
    }
    
    public Optional<Effect> findEffectByPlatformId(String platformId) {
        return effectRepository.findByPlatformId(platformId);
    }
    
    // ========== HASHTAG ==========
    
    @Transactional
    public Hashtag findOrCreateHashtag(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        
        // Normalize tag (remove # if present, lowercase)
        String normalizedTag = tag.startsWith("#") ? tag.substring(1) : tag;
        normalizedTag = normalizedTag.toLowerCase().trim();
        
        if (normalizedTag.isBlank()) {
            return null;
        }
        
        final String finalTag = normalizedTag;
        
        return hashtagRepository.findByTag(finalTag)
                .map(existing -> {
                    existing.incrementUsageCount();
                    return hashtagRepository.save(existing);
                })
                .orElseGet(() -> {
                    Hashtag hashtag = Hashtag.builder()
                            .tag(finalTag)
                            .usageCount(1)
                            .build();
                    log.debug("Created new hashtag: #{}", finalTag);
                    return hashtagRepository.save(hashtag);
                });
    }
    
    public Optional<Hashtag> findHashtagByTag(String tag) {
        String normalizedTag = tag.startsWith("#") ? tag.substring(1) : tag;
        return hashtagRepository.findByTag(normalizedTag.toLowerCase());
    }
    
    // ========== PLACE ==========
    
    @Transactional
    public Place findOrCreatePlace(String platformId, String name) {
        if ((platformId == null || platformId.isBlank()) && 
            (name == null || name.isBlank())) {
            return null;
        }
        
        if (platformId != null && !platformId.isBlank()) {
            Optional<Place> existing = placeRepository.findByPlatformId(platformId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        
        Place place = Place.builder()
                .platformId(platformId)
                .name(name)
                .build();
        
        log.debug("Created new place: {}", name);
        return placeRepository.save(place);
    }
    
    public Optional<Place> findPlaceByPlatformId(String platformId) {
        return placeRepository.findByPlatformId(platformId);
    }
}
