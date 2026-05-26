package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * DTO de auto-cadastro público de um novo Tenant (Empresa + Gerente inicial).
 * O CNPJ passa por validação de formato E de dígitos verificadores.
 */
public record TenantRegistrationRequest(

    // ── Dados fiscais da empresa ─────────────────────────────────────────────

    @NotBlank(message = "CNPJ é obrigatório")
    @CNPJ(message = "CNPJ inválido — verifique os dígitos verificadores")
    String cnpj,

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
    String razaoSocial,

    @Size(max = 200)
    String nomeFantasia,

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido")
    String cep,

    @NotBlank(message = "Logradouro é obrigatório")
    @Size(max = 200)
    String logradouro,

    @NotBlank(message = "Número é obrigatório")
    @Size(max = 20)
    String numero,

    @Size(max = 100)
    String complemento,

    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100)
    String bairro,

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100)
    String cidade,

    @NotBlank(message = "UF é obrigatória")
    @Size(min = 2, max = 2, message = "UF deve ter 2 caracteres")
    String uf,

    @Size(max = 10)
    String codigoMunicipioIbge,

    @Email(message = "E-mail fiscal inválido")
    @Size(max = 180)
    String emailFiscal,

    @Size(max = 20)
    String telefoneFiscal,

    // ── Credenciais do gerente inicial ───────────────────────────────────────

    @NotBlank(message = "Nome do gerente é obrigatório")
    @Size(max = 150)
    String gerenteNome,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 180)
    String gerenteEmail,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
    String gerenteSenha
) {}
