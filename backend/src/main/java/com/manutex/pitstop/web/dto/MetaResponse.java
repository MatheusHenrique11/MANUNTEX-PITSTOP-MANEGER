package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.MetaMecanico;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MetaResponse(
    UUID id,
    UUID mecanicoId,
    String mecanicoNome,
    int mes,
    int ano,
    BigDecimal valorMeta,
    Instant createdAt,
    Instant updatedAt
) {
    public static MetaResponse of(MetaMecanico m) {
        return new MetaResponse(
            m.getId(),
            m.getMecanico().getId(),
            m.getMecanico().getFullName(),
            m.getMes(),
            m.getAno(),
            m.getValorMeta(),
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }
}
