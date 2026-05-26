package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.ConsentType;
import com.manutex.pitstop.domain.enums.DsarStatus;
import com.manutex.pitstop.domain.enums.DsarType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Exportação de dados pessoais do titular (Art. 18, II-III LGPD — portabilidade).
 */
public record MyDataExport(
    Instant exportDate,
    UserData userData,
    List<ConsentRecord> consentHistory,
    List<DsarSummary> dsarHistory
) {

    public record UserData(
        UUID id,
        String email,
        String fullName,
        String role,
        String empresa,
        Instant createdAt
    ) {}

    public record ConsentRecord(
        ConsentType type,
        String version,
        boolean accepted,
        Instant date
    ) {}

    public record DsarSummary(
        UUID id,
        DsarType type,
        DsarStatus status,
        Instant requestedAt,
        Instant deadlineAt,
        Instant completedAt
    ) {}
}
