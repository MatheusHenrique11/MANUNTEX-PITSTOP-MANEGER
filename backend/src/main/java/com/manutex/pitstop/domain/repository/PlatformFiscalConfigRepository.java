package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.PlatformFiscalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformFiscalConfigRepository extends JpaRepository<PlatformFiscalConfig, UUID> {

    /** Retorna a configuração fiscal da plataforma (tabela de linha única). */
    Optional<PlatformFiscalConfig> findFirstByOrderByUpdatedAtDesc();
}
