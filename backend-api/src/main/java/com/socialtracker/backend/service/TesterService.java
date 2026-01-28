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
     * FIX: Gestione della Race Condition per evitare crash su richieste simultanee
     */
    @Transactional
    public Tester findOrCreateTester(String username) {
        if (username == null || username.isBlank()) {
            username = "anonymous";
        }

        final String finalUsername = username.trim();

        // 1. Proviamo a trovarlo normalmente
        Optional<Tester> existing = testerRepository.findByUsername(finalUsername);

        if (existing.isPresent()) {
            Tester tester = existing.get();
            tester.setLastActiveAt(LocalDateTime.now());
            return testerRepository.save(tester);
        }

        // 2. Se non esiste, proviamo a crearlo gestendo la concorrenza
        try {
            log.info("Creating new tester: {}", finalUsername);
            Tester newTester = Tester.builder()
                    .username(finalUsername)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            // saveAndFlush forza l'invio al DB subito, facendo scattare l'errore se duplicato
            return testerRepository.saveAndFlush(newTester);

        } catch (Exception e) {
            // 3. CATCH: Se siamo qui, un altro thread ha creato l'utente un millisecondo prima di noi.
            // Invece di crashare, recuperiamo l'utente appena creato dall'altro thread.
            log.info("Race condition detected for tester '{}', recovering...", finalUsername);

            return testerRepository.findByUsername(finalUsername)
                    .map(tester -> {
                        tester.setLastActiveAt(LocalDateTime.now());
                        return testerRepository.save(tester);
                    })
                    .orElseThrow(() -> new RuntimeException("Critical error: Tester creation conflict failed for " + finalUsername));
        }
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
