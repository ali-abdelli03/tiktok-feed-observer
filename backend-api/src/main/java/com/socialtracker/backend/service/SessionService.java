package com.socialtracker.backend.service;

import com.socialtracker.backend.entity.ScrapingSession;
import com.socialtracker.backend.entity.Tester;
import com.socialtracker.backend.repository.ScrapingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing scraping sessions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final ScrapingSessionRepository sessionRepository;
    private final TesterService testerService;
    
    /**
     * Find or create a session by UUID
     */
    @Transactional
    public ScrapingSession findOrCreateSession(String sessionUuidStr, String username) {
        UUID sessionUuid = null;
        
        // Try to parse UUID, create new if invalid
        if (sessionUuidStr != null && !sessionUuidStr.isBlank()) {
            try {
                sessionUuid = UUID.fromString(sessionUuidStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid session UUID: {}, creating new session", sessionUuidStr);
            }
        }
        
        // If we have a valid UUID, try to find existing session
        if (sessionUuid != null) {
            Optional<ScrapingSession> existingSession = sessionRepository.findBySessionUuid(sessionUuid);
            if (existingSession.isPresent()) {
                return existingSession.get();
            }
        }
        
        // Create new session
        Tester tester = testerService.findOrCreateTester(username);
        
        ScrapingSession newSession = ScrapingSession.builder()
                .sessionUuid(sessionUuid != null ? sessionUuid : UUID.randomUUID())
                .tester(tester)
                .build();
        
        log.info("Created new session: {} for tester: {}", newSession.getSessionUuid(), username);
        return sessionRepository.save(newSession);
    }
    
    public Optional<ScrapingSession> findByUuid(UUID uuid) {
        return sessionRepository.findBySessionUuid(uuid);
    }
    
    public List<ScrapingSession> findByTester(String username) {
        return sessionRepository.findByTesterUsername(username);
    }
    
    @Transactional
    public void endSession(UUID sessionUuid) {
        sessionRepository.findBySessionUuid(sessionUuid).ifPresent(session -> {
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Ended session: {}", sessionUuid);
        });
    }
    
    @Transactional
    public void incrementVideoCount(ScrapingSession session) {
        session.incrementVideoCount();
        sessionRepository.save(session);
    }
    
    @Transactional
    public void incrementCommentCount(ScrapingSession session) {
        session.incrementCommentCount();
        sessionRepository.save(session);
    }
    
    @Transactional
    public void incrementInteractionCount(ScrapingSession session) {
        session.incrementInteractionCount();
        sessionRepository.save(session);
    }
    
    public long count() {
        return sessionRepository.count();
    }
}
