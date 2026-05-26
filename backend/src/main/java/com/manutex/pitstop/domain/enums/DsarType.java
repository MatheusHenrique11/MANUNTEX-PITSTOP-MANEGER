package com.manutex.pitstop.domain.enums;

/**
 * Tipos de Solicitação do Titular conforme Art. 18 da LGPD.
 */
public enum DsarType {
    ACCESS,       // Confirmação de tratamento e acesso aos dados (Art. 18, I-II)
    PORTABILITY,  // Portabilidade em formato estruturado (Art. 18, V)
    CORRECTION,   // Correção de dados incompletos, inexatos ou desatualizados (Art. 18, III)
    ERASURE,      // Anonimização, bloqueio ou eliminação (Art. 18, IV-VI)
    OBJECTION,    // Oposição ao tratamento (Art. 18, IX)
    RESTRICTION   // Limitação do tratamento (Art. 18, IV)
}
