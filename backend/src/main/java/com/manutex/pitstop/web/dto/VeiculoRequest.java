package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.validation.Chassi;
import com.manutex.pitstop.domain.validation.Placa;
import com.manutex.pitstop.domain.validation.Renavam;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record VeiculoRequest(

    @NotBlank
    @Placa
    String placa,

    @NotBlank
    @Chassi
    String chassi,

    @NotBlank
    @Renavam
    String renavam,

    @NotBlank
    @Size(max = 60)
    String marca,

    @NotBlank
    @Size(max = 80)
    String modelo,

    @NotNull @Min(1900) @Max(2100)
    Integer anoFabricacao,

    @NotNull @Min(1900) @Max(2100)
    Integer anoModelo,

    @Size(max = 40)
    String cor,

    @NotNull
    UUID clienteId
) {}
