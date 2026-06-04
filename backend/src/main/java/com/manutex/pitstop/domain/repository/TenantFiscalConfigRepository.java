package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.TenantFiscalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantFiscalConfigRepository extends JpaRepository<TenantFiscalConfig, UUID> {

    Optional<TenantFiscalConfig> findByEmpresaId(UUID empresaId);

    boolean existsByEmpresaId(UUID empresaId);
}
