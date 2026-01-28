package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {
    
    List<CommentMention> findByCommentId(Long commentId);
    
    @Query("SELECT cm FROM CommentMention cm WHERE cm.displayName = :displayName")
    List<CommentMention> findByDisplayName(@Param("displayName") String displayName);
    
    @Query("SELECT cm.displayName, COUNT(cm) FROM CommentMention cm GROUP BY cm.displayName ORDER BY COUNT(cm) DESC")
    List<Object[]> findMostMentionedUsers();
}
