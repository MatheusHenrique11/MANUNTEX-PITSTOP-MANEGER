package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.enums.StatusManutencao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ManutencaoResponse(
    UUID id,
    UUID veiculoId,
    String veiculoPlaca,
    String veiculoMarca,
    String veiculoModelo,
    String veiculoCor,
    String clienteNome,
    String clienteTelefone,
    UUID mecanicoId,
    String mecanicoNome,
    String descricao,
    Integer kmEntrada,
    Integer kmSaida,
    String relatorio,
    String observacoes,
    BigDecimal orcamento,
    BigDecimal valorFinal,
    StatusManutencao status,
    Instant dataEntrada,
    Instant dataConclusao,
    Instant createdAt,
    Instant updatedAt
) {
    public static ManutencaoResponse of(Manutencao m) {
        var cliente = m.getVeiculo().getCliente();
        return new ManutencaoResponse(
            m.getId(),
            m.getVeiculo().getId(),
            m.getVeiculo().getPlaca(),
            m.getVeiculo().getMarca(),
            m.getVeiculo().getModelo(),
            m.getVeiculo().getCor(),
            cliente != null ? cliente.getNome() : null,
            cliente != null ? cliente.getTelefone() : null,
            m.getMecanico().getId(),
            m.getMecanico().getFullName(),
            m.getDescricao(),
            m.getKmEntrada(),
            m.getKmSaida(),
            m.getRelatorio(),
            m.getObservacoes(),
            m.getOrcamento(),
            m.getValorFinal(),
            m.getStatus(),
            m.getDataEntrada(),
            m.getDataConclusao(),
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }
}
