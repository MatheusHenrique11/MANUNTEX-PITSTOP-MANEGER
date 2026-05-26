package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.DataSubjectRequest;
import com.manutex.pitstop.domain.enums.DsarStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, UUID> {

    List<DataSubjectRequest> findByRequesterUserIdOrderByRequestedAtDesc(UUID userId);

    List<DataSubjectRequest> findByEmpresaIdOrderByRequestedAtDesc(UUID empresaId);

    List<DataSubjectRequest> findByEmpresaIdAndStatusOrderByRequestedAtAsc(UUID empresaId, DsarStatus status);
}
