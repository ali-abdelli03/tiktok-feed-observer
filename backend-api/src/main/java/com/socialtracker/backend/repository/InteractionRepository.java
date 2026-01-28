package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
