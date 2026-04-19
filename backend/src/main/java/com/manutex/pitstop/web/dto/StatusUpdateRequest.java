package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.StatusManutencao;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull StatusManutencao status) {}
