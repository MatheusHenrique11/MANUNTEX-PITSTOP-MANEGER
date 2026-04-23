package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByCpfCnpjAndEmpresaId(String cpfCnpj, UUID empresaId);
    boolean existsByCpfCnpjAndEmpresaId(String cpfCnpj, UUID empresaId);

    @Query("SELECT c FROM Cliente c WHERE c.empresa.id = :empresaId AND " +
           "(LOWER(c.nome) LIKE LOWER(CONCAT('%', :q, '%')) OR c.cpfCnpj LIKE CONCAT('%', :q, '%'))")
    Page<Cliente> searchByEmpresa(@Param("empresaId") UUID empresaId, @Param("q") String q, Pageable pageable);

    Page<Cliente> findByEmpresaId(UUID empresaId, Pageable pageable);
}
