package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.response.ApiResponse;
import com.socialtracker.backend.dto.response.ProfileResponseDto;
import com.socialtracker.backend.entity.Profile;
import com.socialtracker.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for querying profile data
 */
@RestController
@RequestMapping("/api/v2/profiles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProfileApiController {
    
    private final ProfileService profileService;
    
    /**
     * Get all profiles with pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProfileResponseDto>>> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Profile> profiles = profileService.findAll(PageRequest.of(page, size));
        Page<ProfileResponseDto> dtos = profiles.map(profileService::toDto);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Get a single profile by handle
     */
    @GetMapping("/{handle}")
    public ResponseEntity<ApiResponse<ProfileResponseDto>> getProfile(@PathVariable String handle) {
        return profileService.findByHandle(handle)
                .map(profile -> ResponseEntity.ok(ApiResponse.success(profileService.toDto(profile))))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get top profiles by video count
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<ProfileResponseDto>>> getTopProfiles(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Profile> profiles = profileService.findTopByVideoCount(limit);
        List<ProfileResponseDto> dtos = profiles.stream()
                .map(profileService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Search profiles by handle or display name
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProfileResponseDto>>> searchProfiles(@RequestParam String q) {
        List<Profile> profiles = profileService.searchProfiles(q);
        List<ProfileResponseDto> dtos = profiles.stream()
                .map(profileService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}
