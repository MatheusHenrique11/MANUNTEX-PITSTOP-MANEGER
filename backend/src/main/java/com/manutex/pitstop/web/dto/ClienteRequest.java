package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.*;

public record ClienteRequest(

    @NotBlank
    @Size(min = 2, max = 150)
    String nome,

    @NotBlank
    @Pattern(
        regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{11}|\\d{14})$",
        message = "CPF (000.000.000-00) ou CNPJ (00.000.000/0000-00) inválido"
    )
    String cpfCnpj,

    @Size(max = 20)
    @Pattern(regexp = "^\\+?[\\d\\s\\-()]{7,20}$", message = "Telefone inválido")
    String telefone,

    @Email
    @Size(max = 180)
    String email
) {}
