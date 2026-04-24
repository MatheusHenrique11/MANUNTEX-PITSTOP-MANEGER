package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManutencaoRepository extends JpaRepository<Manutencao, UUID> {
    Page<Manutencao> findByVeiculoId(UUID veiculoId, Pageable pageable);
    Page<Manutencao> findByMecanicoId(UUID mecanicoId, Pageable pageable);
    Page<Manutencao> findByStatus(StatusManutencao status, Pageable pageable);
    Page<Manutencao> findByVeiculoIdAndStatus(UUID veiculoId, StatusManutencao status, Pageable pageable);
    Optional<Manutencao> findByTrackingToken(UUID trackingToken);

    @Query("""
        SELECT m FROM Manutencao m
        WHERE m.mecanico.id = :mecanicoId
          AND m.status = 'CONCLUIDA'
          AND EXTRACT(MONTH FROM m.dataConclusao) = :mes
          AND EXTRACT(YEAR  FROM m.dataConclusao) = :ano
        ORDER BY m.dataConclusao DESC
        """)
    List<Manutencao> findConcluidasByMecanicoAndPeriodo(
        @Param("mecanicoId") UUID mecanicoId,
        @Param("mes") int mes,
        @Param("ano") int ano
    );

    @Query("""
        SELECT DISTINCT m.mecanico.id FROM Manutencao m
        WHERE m.mecanico.empresa.id = :empresaId
          AND m.status = 'CONCLUIDA'
          AND EXTRACT(MONTH FROM m.dataConclusao) = :mes
          AND EXTRACT(YEAR  FROM m.dataConclusao) = :ano
        """)
    List<UUID> findMecanicoIdsComProducao(
        @Param("empresaId") UUID empresaId,
        @Param("mes") int mes,
        @Param("ano") int ano
    );
}
