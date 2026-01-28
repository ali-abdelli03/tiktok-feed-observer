package com.socialtracker.backend.service;

import com.socialtracker.backend.entity.Tester;
import com.socialtracker.backend.repository.TesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing testers (scraper users)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TesterService {
    
    private final TesterRepository testerRepository;
    
    /**
     * Find or create a tester by username (upsert pattern)
     */
    @Transactional
    public Tester findOrCreateTester(String username) {
        if (username == null || username.isBlank()) {
            username = "anonymous";
        }
        
        final String finalUsername = username.trim();
        
        return testerRepository.findByUsername(finalUsername)
                .map(tester -> {
                    tester.setLastActiveAt(LocalDateTime.now());
                    return testerRepository.save(tester);
                })
                .orElseGet(() -> {
                    log.info("Creating new tester: {}", finalUsername);
                    Tester newTester = Tester.builder()
                            .username(finalUsername)
                            .build();
                    return testerRepository.save(newTester);
                });
    }
    
    public Optional<Tester> findByUsername(String username) {
        return testerRepository.findByUsername(username);
    }
    
    public List<Tester> findAll() {
        return testerRepository.findAll();
    }
    
    public long count() {
        return testerRepository.count();
    }
    
    @Transactional
    public void updateLastActive(String username) {
        testerRepository.updateLastActiveAt(username, LocalDateTime.now());
    }
}
