package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.enums.StatusManutencao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServicoMecanicoItem(
    UUID manutencaoId,
    String veiculoPlaca,
    String veiculoMarca,
    String veiculoModelo,
    String clienteNome,
    String descricao,
    BigDecimal valorFinal,
    Instant dataConclusao,
    StatusManutencao status
) {
    public static ServicoMecanicoItem of(Manutencao m) {
        var cliente = m.getVeiculo().getCliente();
        return new ServicoMecanicoItem(
            m.getId(),
            m.getVeiculo().getPlaca(),
            m.getVeiculo().getMarca(),
            m.getVeiculo().getModelo(),
            cliente != null ? cliente.getNome() : null,
            m.getDescricao(),
            m.getValorFinal(),
            m.getDataConclusao(),
            m.getStatus()
        );
    }
}
