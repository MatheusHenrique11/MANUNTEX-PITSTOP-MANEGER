package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.NotificationLog;
import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import com.manutex.pitstop.domain.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
    UUID id,
    UUID manutencaoId,
    NotificationEvent evento,
    NotificationChannel canal,
    String destinatario,
    NotificationStatus status,
    String errorMessage,
    Instant enviadoEm,
    Instant createdAt
) {
    public static NotificationLogResponse of(NotificationLog l) {
        return new NotificationLogResponse(
            l.getId(), l.getManutencaoId(), l.getEvento(), l.getCanal(),
            l.getDestinatario(), l.getStatus(), l.getErrorMessage(),
            l.getEnviadoEm(), l.getCreatedAt()
        );
    }
}
