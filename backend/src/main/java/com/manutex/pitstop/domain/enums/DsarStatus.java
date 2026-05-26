package com.manutex.pitstop.domain.enums;

public enum DsarStatus {
    PENDING,      // Aguardando análise (prazo: 15 dias úteis)
    IN_PROGRESS,  // Em processamento
    COMPLETED,    // Concluída
    REJECTED      // Indeferida (com justificativa obrigatória)
}
