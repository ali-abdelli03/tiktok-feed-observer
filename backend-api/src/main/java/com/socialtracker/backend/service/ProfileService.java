package com.socialtracker.backend.service;

import com.socialtracker.backend.dto.ProfileVisitDto;
import com.socialtracker.backend.dto.response.ProfileResponseDto;
import com.socialtracker.backend.entity.Profile;
import com.socialtracker.backend.entity.ProfileVisit;
import com.socialtracker.backend.entity.ScrapingSession;
import com.socialtracker.backend.entity.Tester;
import com.socialtracker.backend.repository.ProfileRepository;
import com.socialtracker.backend.repository.ProfileVisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing TikTok profiles
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    
    private final ProfileRepository profileRepository;
    private final ProfileVisitRepository profileVisitRepository;
    private final TesterService testerService;
    private final SessionService sessionService;
    
    /**
     * Find or create a profile by handle (upsert pattern).
     * Handles race conditions by catching duplicate key exceptions and retrying lookup.
     */
    @Transactional
    public Profile findOrCreateProfile(String platformHandle) {
        if (platformHandle == null || platformHandle.isBlank()) {
            return null;
        }
        
        // Remove @ prefix if present
        String handle = platformHandle.startsWith("@") 
            ? platformHandle.substring(1) 
            : platformHandle;
        
        // First attempt: check if exists
        Optional<Profile> existing = profileRepository.findByPlatformHandle(handle);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Profile doesn't exist, try to create it
        try {
            log.debug("Creating new profile: {}", handle);
            Profile newProfile = Profile.builder()
                    .platformHandle(handle)
                    .build();
            return profileRepository.saveAndFlush(newProfile);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created the profile first
            // Retry lookup - it should exist now
            log.debug("Profile {} was created by concurrent request, fetching existing", handle);
            return profileRepository.findByPlatformHandle(handle)
                    .orElseThrow(() -> new IllegalStateException(
                            "Profile should exist after duplicate key error: " + handle));
        }
    }
    
    /**
     * Update profile with visit data
     */
    @Transactional
    public Profile updateFromVisit(ProfileVisitDto dto) {
        String handle = dto.profile_handle();
        if (handle == null || handle.isBlank()) {
            return null;
        }
        
        // Remove @ prefix
        handle = handle.startsWith("@") ? handle.substring(1) : handle;
        
        Profile profile = findOrCreateProfile(handle);
        
        // Update profile data if available
        if (dto.display_name() != null && !dto.display_name().isBlank()) {
            profile.setDisplayName(dto.display_name());
        }
        if (dto.bio() != null) {
            profile.setBio(dto.bio());
        }
        if (dto.profile_link() != null) {
            profile.setProfileLink(dto.profile_link());
        }
        if (dto.is_verified() != null) {
            profile.setIsVerified(dto.is_verified());
        }
        
        // Parse counts
        profile.setFollowerCount(parseCount(dto.follower_count()));
        profile.setFollowingCount(parseCount(dto.following_count()));
        profile.setLikesCount(parseCount(dto.likes_count()));
        
        profile = profileRepository.save(profile);
        
        // Record visit
        Tester tester = testerService.findOrCreateTester(dto.scraped_by());
        
        ProfileVisit visit = ProfileVisit.builder()
                .tester(tester)
                .profile(profile)
                .contextType(dto.context_type())
                .referrerUrl(dto.referrer_url())
                .build();
        profileVisitRepository.save(visit);
        
        log.info("Updated profile: {} with visit from: {}", handle, dto.scraped_by());
        return profile;
    }
    
    /**
     * Update profile with verification status
     */
    @Transactional
    public void updateVerificationStatus(String handle, boolean isVerified) {
        profileRepository.findByPlatformHandle(handle).ifPresent(profile -> {
            profile.setIsVerified(isVerified);
            profileRepository.save(profile);
        });
    }
    
    public Optional<Profile> findByHandle(String handle) {
        return profileRepository.findByPlatformHandle(handle);
    }
    
    public Page<Profile> findAll(Pageable pageable) {
        return profileRepository.findAll(pageable);
    }
    
    public List<Profile> findTopByVideoCount(int limit) {
        return profileRepository.findTopByVideoCount(PageRequest.of(0, limit)).getContent();
    }
    
    public List<Profile> searchProfiles(String search) {
        return profileRepository.searchProfiles(search);
    }
    
    public long count() {
        return profileRepository.count();
    }
    
    public long countVerified() {
        return profileRepository.countVerifiedProfiles();
    }
    
    /**
     * Convert entity to response DTO
     */
    public ProfileResponseDto toDto(Profile profile) {
        return ProfileResponseDto.builder()
                .id(profile.getId())
                .platformHandle(profile.getPlatformHandle())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .profileLink(profile.getProfileLink())
                .isVerified(profile.getIsVerified())
                .followerCount(profile.getFollowerCount())
                .followingCount(profile.getFollowingCount())
                .likesCount(profile.getLikesCount())
                .videoCount(profile.getVideos() != null ? profile.getVideos().size() : 0)
                .firstSeenAt(profile.getFirstSeenAt())
                .lastUpdatedAt(profile.getLastUpdatedAt())
                .build();
    }
    
    /**
     * Batch convert entities to DTOs
     */
    public List<ProfileResponseDto> toDtoList(List<Profile> profiles) {
        return profiles.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    /**
     * Parse count strings like "1.2M", "500K", "1,234" to Long
     */
    private Long parseCount(String countStr) {
        if (countStr == null || countStr.isBlank()) {
            return null;
        }
        
        try {
            String cleaned = countStr.trim().toUpperCase().replace(",", "");
            
            double multiplier = 1;
            if (cleaned.endsWith("K")) {
                multiplier = 1_000;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("M")) {
                multiplier = 1_000_000;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("B")) {
                multiplier = 1_000_000_000;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            
            double value = Double.parseDouble(cleaned);
            return (long) (value * multiplier);
        } catch (NumberFormatException e) {
            log.debug("Could not parse count: {}", countStr);
            return null;
        }
    }
}
