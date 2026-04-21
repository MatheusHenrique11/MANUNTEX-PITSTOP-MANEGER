package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.enums.StatusManutencao;

import java.time.Instant;
import java.util.UUID;

public record RastreioResponse(
    UUID trackingToken,
    StatusManutencao status,
    String veiculoPlaca,
    String veiculoMarca,
    String veiculoModelo,
    String veiculoCor,
    String mecanicoNome,
    String descricao,
    String observacoes,
    Instant dataEntrada,
    Instant dataConclusao
) {
    public static RastreioResponse of(Manutencao m) {
        String[] partes = m.getMecanico().getFullName().split(" ");
        String primeiroNome = partes[0];

        return new RastreioResponse(
            m.getTrackingToken(),
            m.getStatus(),
            m.getVeiculo().getPlaca(),
            m.getVeiculo().getMarca(),
            m.getVeiculo().getModelo(),
            m.getVeiculo().getCor(),
            primeiroNome,
            m.getDescricao(),
            m.getObservacoes(),
            m.getDataEntrada(),
            m.getDataConclusao()
        );
    }
}
