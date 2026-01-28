package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
