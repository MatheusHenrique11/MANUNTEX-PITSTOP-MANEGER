package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.AuditLog;
import com.manutex.pitstop.domain.repository.AuditLogRepository;
import com.manutex.pitstop.domain.repository.ClienteRepository;
import com.manutex.pitstop.domain.repository.RefreshTokenRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Política de retenção de dados pessoais (Art. 15 LGPD).
 *
 * Executa diariamente às 03:00 e:
 *  - Exclui permanentemente registros com deleted_at > 90 dias (após anonimização)
 *  - Remove tokens de sessão expirados ou revogados
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionService {

    private static final int HARD_DELETE_AFTER_DAYS = 90;

    private final UserRepository         userRepository;
    private final ClienteRepository      clienteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository     auditLogRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    @SuppressWarnings("null")
    public void runRetentionPolicy() {
        Instant cutoff = Instant.now().minus(HARD_DELETE_AFTER_DAYS, ChronoUnit.DAYS);
        Instant now    = Instant.now();

        // Native queries bypass @SQLRestriction so soft-deleted rows are visible
        int deletedUsers    = userRepository.hardDeleteAnonymized(cutoff);
        int deletedClientes = clienteRepository.hardDeleteAnonymized(cutoff);
        int deletedTokens   = refreshTokenRepository.deleteExpiredAndRevoked(now);

        String detail = String.format(
            "users=%d, clientes=%d, tokens=%d (cutoff=%s)",
            deletedUsers, deletedClientes, deletedTokens, cutoff
        );

        auditLogRepository.save(AuditLog.builder()
            .action("RETENTION_CLEANUP")
            .resourceType("SYSTEM")
            .detail(detail)
            .build());

        log.info("Política de retenção executada: {}", detail);
    }
}
