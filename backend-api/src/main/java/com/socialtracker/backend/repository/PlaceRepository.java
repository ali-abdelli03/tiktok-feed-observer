package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    
    Optional<Place> findByPlatformId(String platformId);
    
    Optional<Place> findByName(String name);
    
    boolean existsByPlatformId(String platformId);
}
