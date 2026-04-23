package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    Optional<Veiculo> findByPlaca(String placa);
    Optional<Veiculo> findByChassi(String chassi);
    Optional<Veiculo> findByRenavam(String renavam);

    boolean existsByPlaca(String placa);
    boolean existsByChassi(String chassi);
    boolean existsByRenavam(String renavam);

    Page<Veiculo> findByClienteId(UUID clienteId, Pageable pageable);

    @Query("SELECT v FROM Veiculo v WHERE v.placa LIKE UPPER(CONCAT('%', :termo, '%')) OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :termo, '%'))")
    Page<Veiculo> search(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT v FROM Veiculo v WHERE v.cliente.empresa.id = :empresaId AND " +
           "(v.placa LIKE UPPER(CONCAT('%', :termo, '%')) OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Veiculo> searchByEmpresa(@Param("empresaId") UUID empresaId, @Param("termo") String termo, Pageable pageable);

    Page<Veiculo> findByClienteEmpresaId(UUID empresaId, Pageable pageable);
}
