package com.manutex.pitstop.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manutex.pitstop.domain.entity.Documento;
import com.manutex.pitstop.domain.enums.TipoDocumento;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de documento — storageKey NUNCA é incluído.
 * O frontend recebe apenas a viewUrl temporária quando solicitar visualização.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentoResponse(
    UUID id,
    UUID veiculoId,
    UUID clienteId,
    TipoDocumento tipo,
    String nomeOriginal,
    Long tamanhoBytes,
    String checksumSha256,
    Instant createdAt,
    Instant expiresAt,
    String viewUrl          // preenchido apenas em GET /documentos/{id}/view
) {
    public static DocumentoResponse of(Documento d) {
        return new DocumentoResponse(
            d.getId(),
            d.getVeiculo()  != null ? d.getVeiculo().getId()  : null,
            d.getCliente()  != null ? d.getCliente().getId()  : null,
            d.getTipo(),
            d.getNomeOriginal(),
            d.getTamanhoBytes(),
            d.getChecksumSha256(),
            d.getCreatedAt(),
            d.getExpiresAt(),
            null
        );
    }

    public DocumentoResponse withViewUrl(String url) {
        return new DocumentoResponse(
            id, veiculoId, clienteId, tipo, nomeOriginal,
            tamanhoBytes, checksumSha256, createdAt, expiresAt, url
        );
    }
}
