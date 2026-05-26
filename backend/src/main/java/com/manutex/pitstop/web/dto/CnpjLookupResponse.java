package com.manutex.pitstop.web.dto;

/**
 * Resposta do endpoint de lookup de CNPJ.
 * Os dados vêm da BrasilAPI e são normalizados para uso no formulário de cadastro.
 */
public record CnpjLookupResponse(
    String cnpj,
    String razaoSocial,
    String nomeFantasia,
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String uf,
    String codigoMunicipioIbge,
    String emailFiscal,
    String telefoneFiscal,
    String situacaoCadastral
) {}
