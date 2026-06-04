package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.NotificationTemplate;
import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;

import java.util.UUID;

public record NotificationTemplateResponse(
    UUID id,
    NotificationEvent evento,
    NotificationChannel canal,
    String titulo,
    String corpo,
    boolean ativo
) {
    public static NotificationTemplateResponse of(NotificationTemplate t) {
        return new NotificationTemplateResponse(
            t.getId(), t.getEvento(), t.getCanal(),
            t.getTitulo(), t.getCorpo(), t.isAtivo()
        );
    }
}
