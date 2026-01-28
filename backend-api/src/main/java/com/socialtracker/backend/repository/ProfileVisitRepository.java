package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.ProfileVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileVisitRepository extends JpaRepository<ProfileVisit, Long> {
    
    List<ProfileVisit> findByTesterId(Long testerId);
    
    List<ProfileVisit> findByProfileId(Long profileId);
    
    List<ProfileVisit> findBySessionId(Long sessionId);
    
    @Query("SELECT pv FROM ProfileVisit pv WHERE pv.tester.username = :username ORDER BY pv.visitedAt DESC")
    List<ProfileVisit> findByTesterUsername(@Param("username") String username);
    
    @Query("SELECT pv.profile.platformHandle, COUNT(pv) FROM ProfileVisit pv " +
           "GROUP BY pv.profile.platformHandle ORDER BY COUNT(pv) DESC")
    List<Object[]> findMostVisitedProfiles();
    
    @Query("SELECT COUNT(pv) FROM ProfileVisit pv WHERE pv.tester.id = :testerId")
    long countByTesterId(@Param("testerId") Long testerId);
}
