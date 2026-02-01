package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Comment;
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
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    Optional<Comment> findByCommentHash(String commentHash);
    
    boolean existsByCommentHash(String commentHash);
    
    List<Comment> findByVideoId(Long videoId);
    
    List<Comment> findByVideoPlatformId(String platformId);
    
    Page<Comment> findByVideoIdOrderByCapturedAtDesc(Long videoId, Pageable pageable);
    
    List<Comment> findByAuthorHandle(String authorHandle);
    
    @Query("SELECT c FROM Comment c WHERE c.likedByAuthor = true")
    List<Comment> findLikedByAuthor();
    
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId AND c.likedByAuthor = true")
    List<Comment> findLikedByAuthorForVideo(@Param("videoId") Long videoId);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.video.id = :videoId")
    long countByVideoId(@Param("videoId") Long videoId);
    
    @Query("SELECT c FROM Comment c WHERE c.textContent LIKE %:keyword%")
    List<Comment> searchByContent(@Param("keyword") String keyword);
    
    @Query("SELECT c.authorHandle, COUNT(c) FROM Comment c GROUP BY c.authorHandle ORDER BY COUNT(c) DESC")
    List<Object[]> findTopCommenters(Pageable pageable);
    
    /**
     * Count comments per time bucket for timeline charts
     */
    @Query(value = "WITH time_buckets AS ( " +
           "  SELECT generate_series(0, :bucketCount - 1) AS bucket " +
           ") " +
           "SELECT tb.bucket, COUNT(c.id) " +
           "FROM time_buckets tb " +
           "LEFT JOIN comment c ON " +
           "  CASE " +
           "    WHEN :range = '24h' THEN " +
           "      c.captured_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' hours')::interval " +
           "      AND c.captured_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' hours')::interval " +
           "    ELSE " +
           "      c.captured_at >= CAST(:startTime AS TIMESTAMP) + (tb.bucket || ' days')::interval " +
           "      AND c.captured_at < CAST(:startTime AS TIMESTAMP) + ((tb.bucket + 1) || ' days')::interval " +
           "  END " +
           "GROUP BY tb.bucket " +
           "ORDER BY tb.bucket", nativeQuery = true)
    List<Object[]> countCommentsByTimeBucket(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("bucketCount") int bucketCount,
            @Param("range") String range);
}
