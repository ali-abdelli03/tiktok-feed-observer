package com.socialtracker.backend.repository;

import com.socialtracker.backend.entity.SearchEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchEventRepository extends JpaRepository<SearchEvent, Long> {
    
    List<SearchEvent> findByTesterId(Long testerId);
    
    List<SearchEvent> findBySessionId(Long sessionId);
    
    @Query("SELECT se FROM SearchEvent se WHERE se.tester.username = :username ORDER BY se.searchedAt DESC")
    List<SearchEvent> findByTesterUsername(@Param("username") String username);
    
    @Query("SELECT se.query, COUNT(se) FROM SearchEvent se GROUP BY se.query ORDER BY COUNT(se) DESC")
    List<Object[]> findTopSearchQueries(Pageable pageable);
    
    @Query("SELECT se FROM SearchEvent se WHERE LOWER(se.query) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SearchEvent> searchByQuery(@Param("keyword") String keyword);
}
