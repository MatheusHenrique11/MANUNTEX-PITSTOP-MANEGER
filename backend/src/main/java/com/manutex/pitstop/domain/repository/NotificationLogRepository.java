package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByEmpresaIdOrderByCreatedAtDesc(UUID empresaId, Pageable pageable);

    @Query("""
        SELECT l FROM NotificationLog l
        WHERE l.empresaId = :empresaId
          AND (:status IS NULL OR l.status = :status)
          AND (:canal  IS NULL OR l.canal  = :canal)
          AND (:evento IS NULL OR l.evento = :evento)
        ORDER BY l.createdAt DESC
        """)
    Page<NotificationLog> findFiltered(
        @Param("empresaId") UUID empresaId,
        @Param("status")    com.manutex.pitstop.domain.enums.NotificationStatus status,
        @Param("canal")     com.manutex.pitstop.domain.enums.NotificationChannel canal,
        @Param("evento")    com.manutex.pitstop.domain.enums.NotificationEvent    evento,
        Pageable pageable
    );

    /** LGPD: limpa logs mais antigos que o cutoff (Art. 15 — retenção de dados). */
    @Modifying
    @Query("DELETE FROM NotificationLog l WHERE l.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    /** LGPD: limpa logs vinculados a um cliente anonimizado. */
    @Modifying
    @Query("DELETE FROM NotificationLog l WHERE l.clienteId = :clienteId")
    int deleteByClienteId(@Param("clienteId") UUID clienteId);
}
