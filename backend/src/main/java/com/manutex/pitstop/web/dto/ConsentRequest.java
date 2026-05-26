package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsentRequest(
    @NotNull ConsentType policyType,
    @NotBlank String policyVersion,
    boolean accepted
) {}
