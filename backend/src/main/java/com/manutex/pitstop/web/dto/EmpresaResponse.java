package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(
    UUID id,
    String nome,
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
    boolean ativo,
    SubscriptionStatus subscriptionStatus,
    Instant createdAt
) {
    public static EmpresaResponse of(Empresa e) {
        return new EmpresaResponse(
            e.getId(), e.getNome(), e.getCnpj(),
            e.getRazaoSocial(), e.getNomeFantasia(),
            e.getCep(), e.getLogradouro(), e.getNumero(),
            e.getComplemento(), e.getBairro(), e.getCidade(),
            e.getUf(), e.getCodigoMunicipioIbge(),
            e.getEmailFiscal(), e.getTelefoneFiscal(),
            e.isAtivo(), e.getSubscriptionStatus(), e.getCreatedAt()
        );
    }
}
