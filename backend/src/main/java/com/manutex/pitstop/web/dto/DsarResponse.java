package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.DataSubjectRequest;
import com.manutex.pitstop.domain.enums.DsarStatus;
import com.manutex.pitstop.domain.enums.DsarType;

import java.time.Instant;
import java.util.UUID;

public record DsarResponse(
    UUID id,
    String requesterEmail,
    DsarType requestType,
    DsarStatus status,
    String notes,
    String responseNotes,
    Instant requestedAt,
    Instant deadlineAt,
    Instant completedAt,
    String processedBy
) {
    public static DsarResponse of(DataSubjectRequest r) {
        return new DsarResponse(
            r.getId(), r.getRequesterEmail(), r.getRequestType(),
            r.getStatus(), r.getNotes(), r.getResponseNotes(),
            r.getRequestedAt(), r.getDeadlineAt(), r.getCompletedAt(),
            r.getProcessedBy()
        );
    }
}
