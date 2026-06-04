package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PlatformFiscalConfigRequest(
    @NotBlank @Size(max = 200) String razaoSocial,
    @Size(max = 200) String nomeFantasia,
    @NotBlank @Size(max = 18) String cnpj,
    @Size(max = 30) String inscricaoMunicipal,
    @Size(max = 30) String regimeTributario,
    @NotBlank @Size(max = 10) String codigoMunicipio,
    @Size(max = 100) String municipio,
    @Size(max = 2) String uf,
    @Size(max = 200) String endereco,
    @Size(max = 20) String numero,
    @Size(max = 100) String bairro,
    @Size(max = 10) String cep,
    @Email @Size(max = 180) String emailFiscal,
    @Size(max = 30) String telefoneFiscal,
    @Size(max = 30) String codigoServicoMunicipal,
    @Size(max = 10) String itemListaServico,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal aliquotaIss,
    @NotNull FiscalEnvironment ambienteFiscal
) {}
