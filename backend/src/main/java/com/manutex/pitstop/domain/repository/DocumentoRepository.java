package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Documento;
import com.manutex.pitstop.domain.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    /** Documentos com expiresAt entre agora e o limite — para alertas de vencimento. */
    @Query("""
        SELECT d FROM Documento d
        WHERE d.expiresAt IS NOT NULL
          AND d.expiresAt > :agora
          AND d.expiresAt <= :limite
        ORDER BY d.expiresAt ASC
        """)
    List<Documento> findVencendoEntre(
        @Param("agora")  Instant agora,
        @Param("limite") Instant limite
    );
}
