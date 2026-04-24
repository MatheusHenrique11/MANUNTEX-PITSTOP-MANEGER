package com.manutex.pitstop.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProducaoMecanicoResponse(
    UUID mecanicoId,
    String mecanicoNome,
    int mes,
    int ano,
    int totalServicos,
    BigDecimal totalValorProduzido,
    BigDecimal valorMeta,
    double percentualAtingido,
    boolean metaBatida,
    List<ServicoMecanicoItem> servicos
) {}
