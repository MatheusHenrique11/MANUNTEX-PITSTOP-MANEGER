package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.EmpresaConfig;

import java.util.UUID;

public record EmpresaConfigResponse(
    UUID id,
    String nome,
    String cnpj,
    String endereco,
    String telefone,
    String email,
    String logoUrl
) {
    public static EmpresaConfigResponse of(EmpresaConfig e, String logoUrl) {
        return new EmpresaConfigResponse(
            e.getId(),
            e.getNome(),
            e.getCnpj(),
            e.getEndereco(),
            e.getTelefone(),
            e.getEmail(),
            logoUrl
        );
    }
}
