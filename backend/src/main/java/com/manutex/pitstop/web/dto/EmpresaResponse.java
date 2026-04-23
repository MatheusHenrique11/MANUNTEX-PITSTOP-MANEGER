package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Empresa;

import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(
    UUID id,
    String nome,
    String cnpj,
    boolean ativo,
    Instant createdAt
) {
    public static EmpresaResponse of(Empresa e) {
        return new EmpresaResponse(e.getId(), e.getNome(), e.getCnpj(), e.isAtivo(), e.getCreatedAt());
    }
}
