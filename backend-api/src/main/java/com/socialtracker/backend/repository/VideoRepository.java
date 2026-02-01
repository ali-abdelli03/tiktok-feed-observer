package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    
    Optional<Video> findByPlatformId(String platformId);
    
    boolean existsByPlatformId(String platformId);
    
    List<Video> findByProfileId(Long profileId);
    
    List<Video> findByProfilePlatformHandle(String platformHandle);
    
    Page<Video> findByIsAdTrue(Pageable pageable);
    
    Page<Video> findByIsLiveTrue(Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.firstSeenAt >= :since ORDER BY v.firstSeenAt DESC")
    List<Video> findRecentVideos(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT v FROM Video v LEFT JOIN v.stats s WHERE s.capturedAt = " +
           "(SELECT MAX(s2.capturedAt) FROM VideoStats s2 WHERE s2.video = v) " +
           "ORDER BY s.likes DESC")
    Page<Video> findTopByLikes(Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.description LIKE %:keyword%")
    List<Video> searchByDescription(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(v) FROM Video v WHERE v.isAd = true")
    long countAds();
    
    @Query("SELECT COUNT(v) FROM Video v WHERE v.isLive = true")
    long countLives();
    
    @Query("SELECT v FROM Video v JOIN v.hashtags h WHERE h.tag = :tag")
    List<Video> findByHashtag(@Param("tag") String tag);
    
    @Query("SELECT v FROM Video v ORDER BY v.firstSeenAt DESC")
    Page<Video> findAllOrderByFirstSeenAtDesc(Pageable pageable);
    
    // ==================== ADVANCED SEARCH QUERIES ====================
    
    /**
     * Search videos by author handle with optional filters
     */
    @Query("SELECT DISTINCT v FROM Video v " +
           "WHERE v.profile.platformHandle = :handle " +
           "AND (:excludeAds = false OR v.isAd = false) " +
           "AND (:excludeAi = false OR v.isAi = false) " +
           "ORDER BY v.firstSeenAt DESC")
    List<Video> findByAuthorHandle(
            @Param("handle") String handle,
            @Param("excludeAds") boolean excludeAds,
            @Param("excludeAi") boolean excludeAi);
    
    /**
     * Search videos by author handle containing keyword
     */
    @Query("SELECT DISTINCT v FROM Video v " +
           "WHERE LOWER(v.profile.platformHandle) LIKE LOWER(CONCAT('%', :handle, '%')) " +
           "AND (:excludeAds = false OR v.isAd = false) " +
           "AND (:excludeAi = false OR v.isAi = false) " +
           "ORDER BY v.firstSeenAt DESC")
    List<Video> findByAuthorHandleContaining(
            @Param("handle") String handle,
            @Param("excludeAds") boolean excludeAds,
            @Param("excludeAi") boolean excludeAi);
    
    /**
     * Search videos by hashtag with filters
     */
    @Query("SELECT DISTINCT v FROM Video v JOIN v.hashtags h " +
           "WHERE LOWER(h.tag) = LOWER(:hashtag) " +
           "AND (:excludeAds = false OR v.isAd = false) " +
           "AND (:excludeAi = false OR v.isAi = false) " +
           "ORDER BY v.firstSeenAt DESC")
    List<Video> findByHashtagFiltered(
            @Param("hashtag") String hashtag,
            @Param("excludeAds") boolean excludeAds,
            @Param("excludeAi") boolean excludeAi);
    
    /**
     * Search videos by description keyword with filters
     */
    @Query("SELECT DISTINCT v FROM Video v " +
           "WHERE LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND (:excludeAds = false OR v.isAd = false) " +
           "AND (:excludeAi = false OR v.isAi = false) " +
           "ORDER BY v.firstSeenAt DESC")
    List<Video> searchByDescriptionFiltered(
            @Param("keyword") String keyword,
            @Param("excludeAds") boolean excludeAds,
            @Param("excludeAi") boolean excludeAi);
    
    /**
     * Combined search: author + hashtag + keyword with all filters
     */
    @Query(value = "SELECT DISTINCT v.* FROM video v " +
           "LEFT JOIN profile p ON v.profile_id = p.id " +
           "LEFT JOIN video_hashtag vh ON v.id = vh.video_id " +
           "LEFT JOIN hashtag h ON vh.hashtag_id = h.id " +
           "WHERE (CAST(:handle AS TEXT) IS NULL OR CAST(:handle AS TEXT) = '' OR LOWER(p.platform_handle) LIKE LOWER(CONCAT('%', CAST(:handle AS TEXT), '%'))) " +
           "AND (CAST(:hashtag AS TEXT) IS NULL OR CAST(:hashtag AS TEXT) = '' OR LOWER(h.tag) = LOWER(CAST(:hashtag AS TEXT))) " +
           "AND (CAST(:keyword AS TEXT) IS NULL OR CAST(:keyword AS TEXT) = '' OR LOWER(v.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%'))) " +
           "AND (:excludeAds = false OR v.is_ad = false) " +
           "AND (:excludeAi = false OR v.is_ai = false) " +
           "AND (:verifiedOnly = false OR p.is_verified = true) " +
           "AND (CAST(:since AS TIMESTAMP) IS NULL OR v.first_seen_at >= CAST(:since AS TIMESTAMP)) " +
           "ORDER BY v.first_seen_at DESC", nativeQuery = true)
    List<Video> advancedSearch(
            @Param("handle") String handle,
            @Param("hashtag") String hashtag,
            @Param("keyword") String keyword,
            @Param("excludeAds") boolean excludeAds,
            @Param("excludeAi") boolean excludeAi,
            @Param("verifiedOnly") boolean verifiedOnly,
            @Param("since") LocalDateTime since);
    
    /**
     * Find videos with music in date range
     */
    @Query("SELECT v FROM Video v WHERE v.music IS NOT NULL AND v.firstSeenAt >= :since ORDER BY v.firstSeenAt DESC")
    List<Video> findVideosWithMusicSince(@Param("since") LocalDateTime since);
    
    /**
     * Find videos with effects in date range
     */
    @Query("SELECT v FROM Video v WHERE v.effect IS NOT NULL AND v.firstSeenAt >= :since ORDER BY v.firstSeenAt DESC")
    List<Video> findVideosWithEffectsSince(@Param("since") LocalDateTime since);
    
    /**
     * Find videos with places in date range
     */
    @Query("SELECT v FROM Video v WHERE v.place IS NOT NULL AND v.firstSeenAt >= :since ORDER BY v.firstSeenAt DESC")
    List<Video> findVideosWithPlacesSince(@Param("since") LocalDateTime since);
}
