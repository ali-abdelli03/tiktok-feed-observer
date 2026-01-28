package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.Tester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TesterRepository extends JpaRepository<Tester, Long> {
    
    Optional<Tester> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    @Modifying
    @Query("UPDATE Tester t SET t.lastActiveAt = :time WHERE t.username = :username")
    void updateLastActiveAt(@Param("username") String username, @Param("time") LocalDateTime time);
    
    @Query("SELECT COUNT(t) FROM Tester t")
    long countTesters();
}
