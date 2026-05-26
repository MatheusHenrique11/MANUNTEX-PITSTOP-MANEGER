package com.manutex.pitstop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de billing lidas do application.yml (prefixo "billing").
 * O registro via @EnableConfigurationProperties em SecurityConfig ou em
 * PitStopApplication elimina o aviso "Property billing is not allowed" do IDE
 * e garante validação em tempo de startup.
 */
@ConfigurationProperties(prefix = "billing")
public record BillingProperties(Stripe stripe, Nfe nfe) {

    public record Stripe(String secretKey, String webhookSecret) {
        public boolean isConfigured() { return secretKey != null && !secretKey.isBlank(); }
    }

    public record Nfe(String apiUrl, String apiToken, String municipioPrestador) {
        public boolean isConfigured() { return apiToken != null && !apiToken.isBlank(); }
    }
}
