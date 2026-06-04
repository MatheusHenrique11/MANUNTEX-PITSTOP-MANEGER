package com.manutex.pitstop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de billing lidas do application.yml (prefixo "billing").
 *
 * Stripe price IDs são criados no Stripe Dashboard e configurados via env vars.
 * Platform fiscal é fallback quando PlatformFiscalConfig não está cadastrado no banco.
 */
@ConfigurationProperties(prefix = "billing")
public record BillingProperties(Stripe stripe, Nfe nfe, PlatformFiscal platformFiscal) {

    public record Stripe(
        String secretKey,
        String webhookSecret,
        String priceStarter,
        String priceProfessional,
        String priceEnterprise
    ) {
        public boolean isConfigured() {
            return secretKey != null && !secretKey.isBlank();
        }

        public boolean isWebhookConfigured() {
            return webhookSecret != null && !webhookSecret.isBlank();
        }

        public boolean arePriceIdsConfigured() {
            return priceStarter != null && !priceStarter.isBlank()
                && priceProfessional != null && !priceProfessional.isBlank()
                && priceEnterprise != null && !priceEnterprise.isBlank();
        }

        /** Chave de teste Stripe começa com sk_test_. Chave real começa com sk_live_. */
        public boolean isTestKey() {
            return secretKey != null && secretKey.startsWith("sk_test_");
        }
    }

    public record Nfe(String apiUrl, String apiToken, String municipioPrestador) {
        public boolean isConfigured() {
            return apiToken != null && !apiToken.isBlank();
        }

        public boolean isProductionUrl() {
            return apiUrl != null && apiUrl.contains("api.focusnfe.com.br");
        }
    }

    /**
     * Fallback de dados fiscais da RiseCode Studio quando PlatformFiscalConfig
     * não estiver configurado no banco. Em produção, usar a tabela platform_fiscal_config.
     */
    public record PlatformFiscal(
        String cnpj,
        String razaoSocial,
        String inscricaoMunicipal,
        String codigoMunicipio,
        String codigoServicoMunicipal,
        String itemListaServico,
        String aliquotaIss
    ) {
        public boolean hasMinimumData() {
            return cnpj != null && !cnpj.isBlank()
                && razaoSocial != null && !razaoSocial.isBlank()
                && codigoMunicipio != null && !codigoMunicipio.isBlank();
        }
    }
}
