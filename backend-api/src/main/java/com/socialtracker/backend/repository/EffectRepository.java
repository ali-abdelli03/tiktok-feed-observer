package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Effect;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EffectRepository extends JpaRepository<Effect, Long> {
    
    Optional<Effect> findByPlatformId(String platformId);
    
    Optional<Effect> findByName(String name);
    
    boolean existsByPlatformId(String platformId);
    
    @Query("SELECT e FROM Effect e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Effect> searchByName(@Param("search") String search);
    
    /**
     * Get top effects by usage count
     */
    @Query("SELECT e, COUNT(v) as cnt FROM Effect e JOIN Video v ON v.effect = e " +
           "GROUP BY e ORDER BY cnt DESC")
    List<Object[]> findTopEffectsByUsage(Pageable pageable);
    
    /**
     * Get effects used in videos since a date
     */
    @Query("SELECT e, COUNT(v) as cnt FROM Effect e JOIN Video v ON v.effect = e " +
           "WHERE v.firstSeenAt >= :since " +
           "GROUP BY e ORDER BY cnt DESC")
    List<Object[]> findTopEffectsSince(@Param("since") LocalDateTime since, Pageable pageable);
}
