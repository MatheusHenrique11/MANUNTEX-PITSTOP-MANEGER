package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.MetaMecanico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaMecanicoRepository extends JpaRepository<MetaMecanico, UUID> {

    Optional<MetaMecanico> findByMecanicoIdAndMesAndAno(UUID mecanicoId, int mes, int ano);

    List<MetaMecanico> findByEmpresaIdAndMesAndAno(UUID empresaId, int mes, int ano);

    List<MetaMecanico> findByMecanicoIdAndEmpresaId(UUID mecanicoId, UUID empresaId);
}
