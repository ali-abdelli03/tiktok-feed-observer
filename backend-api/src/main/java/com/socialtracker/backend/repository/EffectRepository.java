package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Effect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EffectRepository extends JpaRepository<Effect, Long> {
    
    Optional<Effect> findByPlatformId(String platformId);
    
    Optional<Effect> findByName(String name);
    
    boolean existsByPlatformId(String platformId);
}
