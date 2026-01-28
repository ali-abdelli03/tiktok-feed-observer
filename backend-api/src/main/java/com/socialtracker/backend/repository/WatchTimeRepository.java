package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.WatchTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchTimeRepository extends JpaRepository<WatchTime, Long> {
    
    List<WatchTime> findByVideoId(Long videoId);
    
    List<WatchTime> findBySessionId(Long sessionId);
    
    @Query("SELECT SUM(wt.durationMs) FROM WatchTime wt WHERE wt.video.id = :videoId")
    Long getTotalWatchTimeForVideo(@Param("videoId") Long videoId);
    
    @Query("SELECT AVG(wt.durationMs) FROM WatchTime wt WHERE wt.video.id = :videoId")
    Double getAverageWatchTimeForVideo(@Param("videoId") Long videoId);
    
    @Query("SELECT SUM(wt.durationMs) FROM WatchTime wt WHERE wt.session.id = :sessionId")
    Long getTotalWatchTimeForSession(@Param("sessionId") Long sessionId);
    
    @Query("SELECT wt.video.id, SUM(wt.durationMs) as totalTime FROM WatchTime wt " +
           "GROUP BY wt.video.id ORDER BY totalTime DESC")
    List<Object[]> findTopWatchedVideos();
}
