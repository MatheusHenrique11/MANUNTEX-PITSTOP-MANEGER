package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.Documento;
import com.manutex.pitstop.domain.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    List<Documento> findByVeiculoId(UUID veiculoId);
    List<Documento> findByClienteId(UUID clienteId);
    boolean existsByStorageKey(String storageKey);
    List<Documento> findByVeiculoIdAndTipo(UUID veiculoId, TipoDocumento tipo);
}
