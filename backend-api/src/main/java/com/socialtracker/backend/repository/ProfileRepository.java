package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    
    Optional<Profile> findByPlatformHandle(String platformHandle);
    
    boolean existsByPlatformHandle(String platformHandle);
    
    List<Profile> findByIsVerifiedTrue();
    
    @Query("SELECT p FROM Profile p WHERE p.followerCount >= :minFollowers ORDER BY p.followerCount DESC")
    List<Profile> findTopByFollowerCount(@Param("minFollowers") Long minFollowers, Pageable pageable);
    
    @Query("SELECT p FROM Profile p ORDER BY SIZE(p.videos) DESC")
    Page<Profile> findTopByVideoCount(Pageable pageable);
    
    @Query("SELECT p FROM Profile p WHERE LOWER(p.platformHandle) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Profile> searchProfiles(@Param("search") String search);
    
    @Query("SELECT COUNT(p) FROM Profile p WHERE p.isVerified = true")
    long countVerifiedProfiles();
    
    // ==================== ADVANCED SEARCH QUERIES ====================
    
    /**
     * Advanced profile search with filters
     */
    @Query(value = "SELECT * FROM profile p " +
           "WHERE (:search IS NULL OR :search = '' " +
           "       OR LOWER(CAST(p.platform_handle AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) " +
           "       OR LOWER(CAST(p.display_name AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))) " +
           "AND (:verifiedOnly = false OR p.is_verified = true) " +
           "ORDER BY p.last_updated_at DESC", nativeQuery = true)
    List<Profile> advancedSearch(
            @Param("search") String search,
            @Param("verifiedOnly") boolean verifiedOnly);
    
    /**
     * Find profiles ordered by most recent activity (lastUpdatedAt)
     */
    @Query("SELECT p FROM Profile p ORDER BY p.lastUpdatedAt DESC")
    List<Profile> findAllOrderByRecentActivity(Pageable pageable);
    
    /**
     * Find profiles that have videos, ordered by latest video
     */
    @Query("SELECT DISTINCT p FROM Profile p JOIN p.videos v " +
           "WHERE (:verifiedOnly = false OR p.isVerified = true) " +
           "ORDER BY v.firstSeenAt DESC")
    List<Profile> findActiveCreators(@Param("verifiedOnly") boolean verifiedOnly, Pageable pageable);
}
