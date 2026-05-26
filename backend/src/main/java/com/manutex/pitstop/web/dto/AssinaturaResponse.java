package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record AssinaturaResponse(
    UUID id,
    SubscriptionPlan plano,
    SubscriptionStatus status,
    String gatewaySubscriptionId,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Instant trialEnd,
    Instant createdAt
) {}
