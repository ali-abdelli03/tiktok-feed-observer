package com.socialtracker.backend.service;

import com.socialtracker.backend.entity.Tester;
import com.socialtracker.backend.repository.TesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    // Self-injection is needed to call a @Transactional method from within the same class
    // This ensures the Spring Proxy intercepts the call to handle the transaction boundary
    @Autowired
    @Lazy
    private TesterService self;

    /**
     * Find or create a tester by username (upsert pattern)
     * Handles Race Conditions and Hibernate Session flushing issues
     */
    @Transactional
    public Tester findOrCreateTester(String username) {
        if (username == null || username.isBlank()) {
            username = "anonymous";
        }

        final String finalUsername = username.trim();

        // 1. Try to find it normally
        Optional<Tester> existing = testerRepository.findByUsername(finalUsername);

        if (existing.isPresent()) {
            Tester tester = existing.get();
            tester.setLastActiveAt(LocalDateTime.now());
            return testerRepository.save(tester);
        }

        // 2. If it doesn't exist, try to create it using an ISOLATED transaction
        try {
            // We call the method via 'self' to start a new transaction.
            // If this fails, only the inner transaction rolls back, keeping the current session clean.
            return self.createTesterSafely(finalUsername);

        } catch (Exception e) {
            // 3. CATCH: If we are here, another thread created the user milliseconds before us.
            // Since we used REQUIRES_NEW, the Hibernate Session is not "dirty" with the failed entity.
            log.info("Race condition detected for tester '{}', recovering...", finalUsername);

            return testerRepository.findByUsername(finalUsername)
                    .map(tester -> {
                        tester.setLastActiveAt(LocalDateTime.now());
                        return testerRepository.save(tester);
                    })
                    .orElseThrow(() -> new RuntimeException("Critical error: Tester creation conflict failed for " + finalUsername));
        }
    }

    /**
     * Helper method to create a tester in an isolated transaction.
     * Propagation.REQUIRES_NEW suspends the current transaction and starts a fresh one.
     * If this fails (Duplicate Key), it rolls back cleanly without affecting the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Tester createTesterSafely(String username) {
        log.info("Creating new tester (Isolated Transaction): {}", username);
        Tester newTester = Tester.builder()
                .username(username)
                .lastActiveAt(LocalDateTime.now())
                .build();
        // saveAndFlush forces the DB constraints to be checked immediately
        return testerRepository.saveAndFlush(newTester);
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