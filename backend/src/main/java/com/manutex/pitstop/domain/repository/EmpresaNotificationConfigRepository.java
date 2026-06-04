package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.EmpresaNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaNotificationConfigRepository extends JpaRepository<EmpresaNotificationConfig, UUID> {
    Optional<EmpresaNotificationConfig> findByEmpresaId(UUID empresaId);
}
