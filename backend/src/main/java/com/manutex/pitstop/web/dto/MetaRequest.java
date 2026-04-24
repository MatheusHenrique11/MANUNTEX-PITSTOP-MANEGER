package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record MetaRequest(

    @NotNull(message = "mecanicoId é obrigatório")
    UUID mecanicoId,

    @Min(value = 1, message = "Mês deve ser entre 1 e 12")
    @Max(value = 12, message = "Mês deve ser entre 1 e 12")
    int mes,

    @Min(value = 2020, message = "Ano deve ser a partir de 2020")
    int ano,

    @NotNull(message = "valorMeta é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor da meta deve ser maior que zero")
    @Digits(integer = 10, fraction = 2)
    BigDecimal valorMeta
) {}
