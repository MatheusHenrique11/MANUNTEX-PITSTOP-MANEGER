package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
    @NotNull SubscriptionPlan plano,
    String successUrl,
    String cancelUrl
) {}
