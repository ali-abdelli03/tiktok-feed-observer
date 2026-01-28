package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.VideoStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoStatsRepository extends JpaRepository<VideoStats, Long> {
    
    List<VideoStats> findByVideoIdOrderByCapturedAtDesc(Long videoId);
    
    @Query("SELECT vs FROM VideoStats vs WHERE vs.video.id = :videoId ORDER BY vs.capturedAt DESC LIMIT 1")
    Optional<VideoStats> findLatestByVideoId(@Param("videoId") Long videoId);
    
    @Query("SELECT vs FROM VideoStats vs WHERE vs.video.platformId = :platformId ORDER BY vs.capturedAt DESC LIMIT 1")
    Optional<VideoStats> findLatestByPlatformId(@Param("platformId") String platformId);
    
    @Query("SELECT vs FROM VideoStats vs WHERE vs.session.id = :sessionId")
    List<VideoStats> findBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT SUM(vs.likes), SUM(vs.comments), SUM(vs.shares), SUM(vs.saves) " +
           "FROM VideoStats vs WHERE vs.video.id = :videoId AND vs.capturedAt = " +
           "(SELECT MAX(vs2.capturedAt) FROM VideoStats vs2 WHERE vs2.video.id = :videoId)")
    Object[] getLatestStatsSummary(@Param("videoId") Long videoId);
    
    @Query("SELECT COUNT(vs) FROM VideoStats vs WHERE vs.video.id = :videoId")
    long countStatEntriesForVideo(@Param("videoId") Long videoId);
}
