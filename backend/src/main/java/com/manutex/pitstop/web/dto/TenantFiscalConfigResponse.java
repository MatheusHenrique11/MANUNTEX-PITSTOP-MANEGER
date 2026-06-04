package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.FiscalEnvironment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TenantFiscalConfigResponse(
    UUID id,
    UUID empresaId,
    String razaoSocial,
    String nomeFantasia,
    String cnpjMascarado,
    String inscricaoMunicipal,
    String regimeTributario,
    String codigoMunicipio,
    String municipio,
    String uf,
    String endereco,
    String numero,
    String bairro,
    String cep,
    String emailFiscal,
    String telefoneFiscal,
    String codigoServicoMunicipal,
    String itemListaServico,
    BigDecimal aliquotaIss,
    FiscalEnvironment ambienteFiscal,
    boolean fiscalEnabled,
    boolean readyForEmission,
    Instant fiscalValidatedAt,
    Instant updatedAt
) {}
