package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.Size;

public record NotificationConfigRequest(
    @Size(max = 500) String whatsappProviderUrl,
    @Size(max = 500) String whatsappApiToken,
    @Size(max = 100) String whatsappInstanceName,
    @Size(max = 180) String notificationEmailFrom
) {}
