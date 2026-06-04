package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.EmpresaNotificationConfig;

public record NotificationConfigResponse(
    String whatsappProviderUrl,
    boolean whatsappConfigured,
    String notificationEmailFrom,
    boolean emailConfigured
) {
    public static NotificationConfigResponse of(EmpresaNotificationConfig c) {
        boolean wpp   = c.getWhatsappProviderUrl()   != null && !c.getWhatsappProviderUrl().isBlank()
                     && c.getWhatsappApiToken()       != null && !c.getWhatsappApiToken().isBlank()
                     && c.getWhatsappInstanceName()   != null && !c.getWhatsappInstanceName().isBlank();
        boolean email = c.getNotificationEmailFrom() != null && !c.getNotificationEmailFrom().isBlank();

        return new NotificationConfigResponse(
            c.getWhatsappProviderUrl(),
            wpp,
            c.getNotificationEmailFrom(),
            email
        );
    }
}
