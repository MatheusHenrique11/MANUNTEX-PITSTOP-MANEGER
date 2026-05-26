package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEmpresaIdOrderByCreatedAtDesc(UUID empresaId);

    List<AuditLog> findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID empresaId, Instant from, Instant to);
}
