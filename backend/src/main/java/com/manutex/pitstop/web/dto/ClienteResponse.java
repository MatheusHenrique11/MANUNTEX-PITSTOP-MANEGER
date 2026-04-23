package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Cliente;

import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String nome,
    String cpfCnpj,
    String telefone,
    String email
) {
    public static ClienteResponse of(Cliente c, boolean exposeConfidential) {
        String doc = exposeConfidential ? c.getCpfCnpj() : mask(c.getCpfCnpj());
        return new ClienteResponse(c.getId(), c.getNome(), doc, c.getTelefone(), c.getEmail());
    }

    private static String mask(String doc) {
        if (doc == null || doc.length() < 4) return "***";
        return "***" + doc.substring(doc.length() - 4);
    }
}
