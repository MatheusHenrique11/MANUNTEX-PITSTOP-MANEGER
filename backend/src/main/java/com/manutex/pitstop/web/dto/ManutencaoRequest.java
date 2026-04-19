package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ManutencaoRequest(
    @NotNull UUID veiculoId,
    @NotNull UUID mecanicoId,
    @NotBlank @Size(min = 10, max = 2000) String descricao,
    @PositiveOrZero Integer kmEntrada,
    @Size(max = 4000) String relatorio,
    @Size(max = 2000) String observacoes,
    @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal orcamento,
    @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal valorFinal,
    @PositiveOrZero Integer kmSaida
) {}
