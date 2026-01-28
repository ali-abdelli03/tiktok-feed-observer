package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.ScrapingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScrapingSessionRepository extends JpaRepository<ScrapingSession, Long> {
    
    Optional<ScrapingSession> findBySessionUuid(UUID sessionUuid);
    
    List<ScrapingSession> findByTesterIdOrderByStartedAtDesc(Long testerId);
    
    @Query("SELECT s FROM ScrapingSession s WHERE s.tester.username = :username ORDER BY s.startedAt DESC")
    List<ScrapingSession> findByTesterUsername(@Param("username") String username);
    
    @Query("SELECT s FROM ScrapingSession s WHERE s.endedAt IS NULL AND s.tester.username = :username")
    Optional<ScrapingSession> findActiveSessionByUsername(@Param("username") String username);
    
    @Query("SELECT COUNT(s) FROM ScrapingSession s WHERE s.tester.id = :testerId")
    long countByTesterId(@Param("testerId") Long testerId);
}
