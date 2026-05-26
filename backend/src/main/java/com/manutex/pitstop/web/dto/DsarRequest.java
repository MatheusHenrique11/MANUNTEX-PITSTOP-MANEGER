package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.DsarType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DsarRequest(
    @NotNull DsarType requestType,
    @Size(max = 1000) String notes
) {}
