package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    
    Optional<Hashtag> findByTag(String tag);
    
    boolean existsByTag(String tag);
    
    @Modifying
    @Query("UPDATE Hashtag h SET h.usageCount = h.usageCount + 1 WHERE h.tag = :tag")
    void incrementUsageCount(@Param("tag") String tag);
    
    @Query("SELECT h FROM Hashtag h ORDER BY h.usageCount DESC")
    List<Hashtag> findTopHashtags(Pageable pageable);
    
    @Query("SELECT h FROM Hashtag h WHERE LOWER(h.tag) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Hashtag> searchByTag(@Param("search") String search);
}
