package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ManutencaoRepository extends JpaRepository<Manutencao, UUID> {
    Page<Manutencao> findByVeiculoId(UUID veiculoId, Pageable pageable);
    Page<Manutencao> findByMecanicoId(UUID mecanicoId, Pageable pageable);
    List<Manutencao> findByStatus(StatusManutencao status);
    Page<Manutencao> findByVeiculoIdAndStatus(UUID veiculoId, StatusManutencao status, Pageable pageable);
}
