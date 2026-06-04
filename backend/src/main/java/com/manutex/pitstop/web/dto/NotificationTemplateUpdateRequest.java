package com.manutex.pitstop.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationTemplateUpdateRequest(
    @Size(max = 200) String titulo,
    @NotBlank @Size(max = 4000) String corpo,
    boolean ativo
) {}
