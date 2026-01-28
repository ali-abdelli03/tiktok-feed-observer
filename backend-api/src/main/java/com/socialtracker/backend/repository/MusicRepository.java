package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {
    
    Optional<Music> findByPlatformId(String platformId);
    
    Optional<Music> findByName(String name);
    
    boolean existsByPlatformId(String platformId);
    
    @Query("SELECT m FROM Music m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Music> searchByName(String search);
}
