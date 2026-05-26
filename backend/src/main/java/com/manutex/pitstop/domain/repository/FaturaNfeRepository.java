package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.FaturaNfe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaturaNfeRepository extends JpaRepository<FaturaNfe, UUID> {

    List<FaturaNfe> findByEmpresaIdOrderByIssueDateDesc(UUID empresaId);

    Optional<FaturaNfe> findByGatewayInvoiceId(String gatewayInvoiceId);

    boolean existsByGatewayInvoiceId(String gatewayInvoiceId);
}
