package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Music;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {
    
    Optional<Music> findByPlatformId(String platformId);
    
    Optional<Music> findByName(String name);
    
    boolean existsByPlatformId(String platformId);
    
    @Query("SELECT m FROM Music m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Music> searchByName(@Param("search") String search);
    
    /**
     * Get top music by usage count (videos using this music)
     */
    @Query("SELECT m, COUNT(v) as cnt FROM Music m JOIN Video v ON v.music = m " +
           "GROUP BY m ORDER BY cnt DESC")
    List<Object[]> findTopMusicByUsage(Pageable pageable);
    
    /**
     * Get music used in videos since a date, ordered by frequency
     */
    @Query("SELECT m, COUNT(v) as cnt FROM Music m JOIN Video v ON v.music = m " +
           "WHERE v.firstSeenAt >= :since " +
           "GROUP BY m ORDER BY cnt DESC")
    List<Object[]> findTopMusicSince(@Param("since") LocalDateTime since, Pageable pageable);
}
