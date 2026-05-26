package com.manutex.pitstop.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Representação mínima do payload de evento do Stripe Webhook.
 * Campos irrelevantes para o negócio são ignorados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeWebhookEvent(
    String id,
    String type,
    Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(EventObject object) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventObject(
        String id,
        String customer,
        String subscription,
        String status,
        @JsonProperty("amount_paid")  Long amountPaid,
        @JsonProperty("amount_due")   Long amountDue,
        String currency
    ) {
        /** Converte centavos (Stripe) para reais (BigDecimal). */
        public BigDecimal amountPaidAsBrl() {
            if (amountPaid == null) return BigDecimal.ZERO;
            return BigDecimal.valueOf(amountPaid).movePointLeft(2);
        }
    }
}
