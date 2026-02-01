package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    
    List<Interaction> findByVideoId(Long videoId);
    
    List<Interaction> findBySessionId(Long sessionId);
    
    @Query("SELECT i FROM Interaction i WHERE i.video.platformId = :platformId ORDER BY i.performedAt DESC")
    List<Interaction> findByVideoPlatformId(@Param("platformId") String platformId);
    
    @Query("SELECT i FROM Interaction i WHERE i.interactionType = :type AND i.isActive = true")
    List<Interaction> findActiveByType(@Param("type") String type);
    
    @Query("SELECT i.interactionType, COUNT(i) FROM Interaction i WHERE i.session.id = :sessionId GROUP BY i.interactionType")
    List<Object[]> countByTypeForSession(@Param("sessionId") Long sessionId);
    
    @Query("SELECT COUNT(i) FROM Interaction i WHERE i.interactionType = 'like' AND i.isActive = true")
    long countActiveLikes();
    
    @Query("SELECT COUNT(i) FROM Interaction i WHERE i.interactionType = 'save' AND i.isActive = true")
    long countActiveSaves();
    
    /**
     * Count interactions per time bucket for timeline charts
     */
    @Query(value = "WITH time_buckets AS ( " +
           "  SELECT generate_series(0, :bucketCount - 1) AS bucket " +
           ") " +
           "SELECT tb.bucket, COUNT(i.id) " +
           "FROM time_buckets tb " +
           "LEFT JOIN interaction i ON " +
           "  CASE " +
           "    WHEN :range = '24h' THEN " +
           "      i.performed_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' hours')::interval " +
           "      AND i.performed_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' hours')::interval " +
           "    ELSE " +
           "      i.performed_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' days')::interval " +
           "      AND i.performed_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' days')::interval " +
           "  END " +
           "GROUP BY tb.bucket " +
           "ORDER BY tb.bucket", nativeQuery = true)
    List<Object[]> countInteractionsByTimeBucket(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("bucketCount") int bucketCount,
            @Param("range") String range);
    
    /**
     * Count likes per time bucket
     */
    @Query(value = "WITH time_buckets AS ( " +
           "  SELECT generate_series(0, :bucketCount - 1) AS bucket " +
           ") " +
           "SELECT tb.bucket, COUNT(i.id) " +
           "FROM time_buckets tb " +
           "LEFT JOIN interaction i ON i.interaction_type = 'like' AND " +
           "  CASE " +
           "    WHEN :range = '24h' THEN " +
           "      i.performed_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' hours')::interval " +
           "      AND i.performed_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' hours')::interval " +
           "    ELSE " +
           "      i.performed_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' days')::interval " +
           "      AND i.performed_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' days')::interval " +
           "  END " +
           "GROUP BY tb.bucket " +
           "ORDER BY tb.bucket", nativeQuery = true)
    List<Object[]> countLikesByTimeBucket(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("bucketCount") int bucketCount,
            @Param("range") String range);
    
    /**
     * Count saves per time bucket
     */
    @Query(value = "WITH time_buckets AS ( " +
           "  SELECT generate_series(0, :bucketCount - 1) AS bucket " +
           ") " +
           "SELECT tb.bucket, COUNT(i.id) " +
           "FROM time_buckets tb " +
           "LEFT JOIN interaction i ON i.interaction_type = 'save' AND " +
           "  CASE " +
           "    WHEN :range = '24h' THEN " +
           "      i.performed_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' hours')::interval " +
           "      AND i.performed_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' hours')::interval " +
           "    ELSE " +
           "      i.performed_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' days')::interval " +
           "      AND i.performed_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' days')::interval " +
           "  END " +
           "GROUP BY tb.bucket " +
           "ORDER BY tb.bucket", nativeQuery = true)
    List<Object[]> countSavesByTimeBucket(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("bucketCount") int bucketCount,
            @Param("range") String range);
}
