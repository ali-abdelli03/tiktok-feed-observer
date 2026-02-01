package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Place;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    
    Optional<Place> findByPlatformId(String platformId);
    
    Optional<Place> findByName(String name);
    
    boolean existsByPlatformId(String platformId);
    
    @Query("SELECT p FROM Place p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Place> searchByName(@Param("search") String search);
    
    /**
     * Get top places by usage count
     */
    @Query("SELECT p, COUNT(v) as cnt FROM Place p JOIN Video v ON v.place = p " +
           "GROUP BY p ORDER BY cnt DESC")
    List<Object[]> findTopPlacesByUsage(Pageable pageable);
    
    /**
     * Get places used in videos since a date
     */
    @Query("SELECT p, COUNT(v) as cnt FROM Place p JOIN Video v ON v.place = p " +
           "WHERE v.firstSeenAt >= :since " +
           "GROUP BY p ORDER BY cnt DESC")
    List<Object[]> findTopPlacesSince(@Param("since") LocalDateTime since, Pageable pageable);
}
