package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.*;

public record EmpresaRequest(

    @NotBlank @Size(max = 150)
    String nome,

    @NotBlank
    @Pattern(
        regexp = "^(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{14})$",
        message = "CNPJ inválido (formato: 00.000.000/0000-00 ou 14 dígitos)"
    )
    String cnpj,

    @NotBlank @Email @Size(max = 180)
    String gerenteEmail,

    @NotBlank @Size(min = 8, max = 72)
    String gerenteSenha,

    @NotBlank @Size(max = 150)
    String gerenteNome
) {}
