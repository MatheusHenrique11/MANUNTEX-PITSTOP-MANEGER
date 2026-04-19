package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Remove tokens de refresh expirados ou revogados do banco periodicamente.
 * Evita crescimento indefinido da tabela refresh_tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Executa às 03:00 todo dia
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int removed = refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        log.info("Cleanup de tokens: {} registro(s) removido(s)", removed);
    }
}
