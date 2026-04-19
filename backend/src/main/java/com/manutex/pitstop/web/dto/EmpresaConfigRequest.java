package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.*;

public record EmpresaConfigRequest(
    @NotBlank @Size(max = 150) String nome,
    @Size(max = 18) String cnpj,
    @Size(max = 500) String endereco,
    @Size(max = 20) String telefone,
    @Email @Size(max = 180) String email
) {}
