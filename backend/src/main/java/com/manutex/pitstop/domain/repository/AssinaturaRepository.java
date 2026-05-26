package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Assinatura;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    Optional<Assinatura> findTopByEmpresaIdOrderByCreatedAtDesc(UUID empresaId);

    Optional<Assinatura> findTopByEmpresaIdAndStatusOrderByCreatedAtDesc(UUID empresaId, SubscriptionStatus status);
}
