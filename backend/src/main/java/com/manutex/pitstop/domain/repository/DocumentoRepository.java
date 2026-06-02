package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Documento;
import com.manutex.pitstop.domain.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    List<Documento> findByVeiculoId(UUID veiculoId);
    List<Documento> findByClienteId(UUID clienteId);
    boolean existsByStorageKey(String storageKey);
    List<Documento> findByVeiculoIdAndTipo(UUID veiculoId, TipoDocumento tipo);

    @Query("""
        SELECT COALESCE(SUM(d.tamanhoBytes), 0)
        FROM Documento d
        WHERE d.uploadedBy.empresa.id = :empresaId
        """)
    Optional<Long> sumTamanhoBytesByEmpresaId(@Param("empresaId") UUID empresaId);
}
