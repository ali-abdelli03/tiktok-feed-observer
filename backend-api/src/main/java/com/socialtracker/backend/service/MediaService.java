package com.socialtracker.backend.service;

import com.socialtracker.backend.entity.*;
import com.socialtracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            Music music = Music.builder()
                    .platformId(platformId)
                    .name(name)
                    .url(url)
                    .build();
            log.debug("Created new music: {}", name);
            return musicRepository.saveAndFlush(music);
        } catch (DataIntegrityViolationException e) {
            log.debug("Music {} was created by concurrent request, fetching existing", platformId);
            return musicRepository.findByPlatformId(platformId).orElse(null);
        }
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
        
        try {
            Effect effect = Effect.builder()
                    .platformId(platformId)
                    .name(name)
                    .url(url)
                    .build();
            log.debug("Created new effect: {}", name);
            return effectRepository.saveAndFlush(effect);
        } catch (DataIntegrityViolationException e) {
            log.debug("Effect {} was created by concurrent request, fetching existing", platformId);
            return effectRepository.findByPlatformId(platformId).orElse(null);
        }
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
        
        // Try to find existing first
        Optional<Hashtag> existing = hashtagRepository.findByTag(finalTag);
        if (existing.isPresent()) {
            Hashtag hashtag = existing.get();
            hashtag.incrementUsageCount();
            return hashtagRepository.save(hashtag);
        }
        
        // Try to create new
        try {
            Hashtag hashtag = Hashtag.builder()
                    .tag(finalTag)
                    .usageCount(1)
                    .build();
            log.debug("Created new hashtag: #{}", finalTag);
            return hashtagRepository.saveAndFlush(hashtag);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created the hashtag first
            log.debug("Hashtag #{} was created by concurrent request, fetching existing", finalTag);
            return hashtagRepository.findByTag(finalTag)
                    .map(h -> {
                        h.incrementUsageCount();
                        return hashtagRepository.save(h);
                    })
                    .orElse(null);
        }
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
        
        try {
            Place place = Place.builder()
                    .platformId(platformId)
                    .name(name)
                    .build();
            log.debug("Created new place: {}", name);
            return placeRepository.saveAndFlush(place);
        } catch (DataIntegrityViolationException e) {
            log.debug("Place {} was created by concurrent request, fetching existing", platformId);
            return placeRepository.findByPlatformId(platformId).orElse(null);
        }
    }
    
    public Optional<Place> findPlaceByPlatformId(String platformId) {
        return placeRepository.findByPlatformId(platformId);
    }
}
