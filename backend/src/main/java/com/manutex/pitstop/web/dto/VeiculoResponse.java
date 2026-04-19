package com.manutex.pitstop.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manutex.pitstop.domain.entity.Veiculo;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta para Veiculo.
 *
 * Chassi e RENAVAM chegam mascarados por padrão.
 * O método {@link #of(Veiculo, boolean)} com {@code exposeConfidential = true}
 * só deve ser chamado em controllers protegidos por @PreAuthorize("hasAnyRole('ADMIN','GERENTE')").
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VeiculoResponse(
    UUID id,
    String placa,
    String chassi,
    String renavam,
    String marca,
    String modelo,
    Integer anoFabricacao,
    Integer anoModelo,
    String cor,
    UUID clienteId,
    Instant createdAt
) {
    private static final String MASKED = "***";

    public static VeiculoResponse of(Veiculo v, boolean exposeConfidential) {
        return VeiculoResponse.builder()
            .id(v.getId())
            .placa(v.getPlaca())
            .chassi(exposeConfidential ? v.getChassi() : mask(v.getChassi(), 4))
            .renavam(exposeConfidential ? v.getRenavam() : mask(v.getRenavam(), 3))
            .marca(v.getMarca())
            .modelo(v.getModelo())
            .anoFabricacao(v.getAnoFabricacao())
            .anoModelo(v.getAnoModelo())
            .cor(v.getCor())
            .clienteId(v.getCliente() != null ? v.getCliente().getId() : null)
            .createdAt(v.getCreatedAt())
            .build();
    }

    /** Expõe apenas os últimos {@code visibleChars} caracteres, o restante vira '*' */
    private static String mask(String value, int visibleChars) {
        if (value == null || value.length() <= visibleChars) return MASKED;
        String suffix = value.substring(value.length() - visibleChars);
        return "*".repeat(value.length() - visibleChars) + suffix;
    }
}
