package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.EmpresaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaConfigRepository extends JpaRepository<EmpresaConfig, UUID> {
    Optional<EmpresaConfig> findFirst();
}
