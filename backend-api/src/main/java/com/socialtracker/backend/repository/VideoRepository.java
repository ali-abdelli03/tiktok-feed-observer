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
}
