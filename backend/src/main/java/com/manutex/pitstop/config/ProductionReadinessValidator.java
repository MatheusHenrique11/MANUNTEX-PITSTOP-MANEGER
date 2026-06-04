package com.manutex.pitstop.config;

import com.manutex.pitstop.domain.entity.PlatformFiscalConfig;
import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import com.manutex.pitstop.domain.repository.PlatformFiscalConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Valida no startup se a aplicação está pronta para operar em produção fiscal e billing.
 *
 * Regras:
 * - Avisos (WARN): configurações ausentes que bloqueiam funcionalidades mas não derrubam o app.
 * - Erro fatal: chave Stripe real + ambiente fiscal PRODUCAO com dados incompletos
 *   (emitiria NFS-e com dados inválidos em produção real).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionReadinessValidator {

    private final BillingProperties billing;
    private final PlatformFiscalConfigRepository platformFiscalRepo;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        List<String> warnings = new ArrayList<>();
        List<String> errors   = new ArrayList<>();

        checkStripe(warnings, errors);
        checkFocusNfe(warnings);
        checkPlatformFiscal(warnings, errors);

        if (!warnings.isEmpty()) {
            log.warn("╔══════════════════════════════════════════════════════╗");
            log.warn("║  PITSTOP — AVISOS DE CONFIGURAÇÃO PARA PRODUÇÃO      ║");
            log.warn("╠══════════════════════════════════════════════════════╣");
            warnings.forEach(w -> log.warn("║  ⚠  {}", w));
            log.warn("╚══════════════════════════════════════════════════════╝");
        }

        if (!errors.isEmpty()) {
            log.error("╔══════════════════════════════════════════════════════╗");
            log.error("║  PITSTOP — ERROS CRÍTICOS DE PRODUÇÃO FISCAL         ║");
            log.error("╠══════════════════════════════════════════════════════╣");
            errors.forEach(e -> log.error("║  ✗  {}", e));
            log.error("╚══════════════════════════════════════════════════════╝");
            throw new ProductionConfigException(
                "Configuração inválida para produção fiscal. Verifique os logs acima.");
        }

        if (warnings.isEmpty()) {
            log.info("[PITSTOP] Validação de produção: todas as configurações OK.");
        }
    }

    private void checkStripe(List<String> warnings, List<String> errors) {
        BillingProperties.Stripe stripe = billing.stripe();
        if (stripe == null) {
            warnings.add("billing.stripe não configurado — operando em modo mock.");
            return;
        }

        if (!stripe.isConfigured()) {
            warnings.add("STRIPE_SECRET_KEY ausente — checkout e webhooks em modo mock.");
        } else if (stripe.isTestKey()) {
            warnings.add("STRIPE_SECRET_KEY é chave de TESTE (sk_test_). Não use em produção real.");
        }

        if (!stripe.isWebhookConfigured()) {
            warnings.add("STRIPE_WEBHOOK_SECRET ausente — webhook aceita sem validação de assinatura (inseguro em produção).");
        }

        if (!stripe.arePriceIdsConfigured()) {
            warnings.add("Price IDs do Stripe ausentes (STRIPE_PRICE_STARTER/PROFESSIONAL/ENTERPRISE) — checkout não funcionará.");
        }

        // Erro fatal: chave real de produção sem webhook secret
        if (stripe.isConfigured() && !stripe.isTestKey() && !stripe.isWebhookConfigured()) {
            errors.add("STRIPE_SECRET_KEY de produção configurada mas STRIPE_WEBHOOK_SECRET ausente. " +
                "Webhooks em produção sem validação de assinatura são INSEGUROS.");
        }
    }

    private void checkFocusNfe(List<String> warnings) {
        BillingProperties.Nfe nfe = billing.nfe();
        if (nfe == null || !nfe.isConfigured()) {
            warnings.add("FOCUS_NFE_TOKEN ausente — emissão de NFS-e em modo mock (sem envio à prefeitura).");
        }
    }

    private void checkPlatformFiscal(List<String> warnings, List<String> errors) {
        Optional<PlatformFiscalConfig> platformOpt = platformFiscalRepo.findFirstByOrderByUpdatedAtDesc();

        if (platformOpt.isEmpty()) {
            checkFallbackFiscalProperties(warnings, errors);
            return;
        }

        PlatformFiscalConfig platform = platformOpt.get();

        if (!platform.isReadyForProduction()) {
            warnings.add("PlatformFiscalConfig incompleta — NFS-e da assinatura SaaS não poderá ser emitida. " +
                "Configure via POST /api/v1/admin/fiscal/platform.");
        }

        // Erro fatal: ambiente PRODUCAO com Focus NFe ativo mas dados fiscais incompletos
        BillingProperties.Nfe nfe = billing.nfe();
        boolean focusAtivo = nfe != null && nfe.isConfigured() && nfe.isProductionUrl();
        if (focusAtivo
            && platform.getAmbienteFiscal() == FiscalEnvironment.PRODUCAO
            && !platform.isReadyForProduction()) {
            errors.add("Ambiente fiscal PRODUCAO com Focus NFe ativo mas PlatformFiscalConfig incompleta. " +
                "Emissão em produção seria rejeitada pela prefeitura.");
        }
    }

    private void checkFallbackFiscalProperties(List<String> warnings, List<String> errors) {
        BillingProperties.PlatformFiscal pf = billing.platformFiscal();
        boolean hasFallback = pf != null && pf.hasMinimumData();

        if (!hasFallback) {
            warnings.add("Nenhum dado fiscal da RiseCode Studio configurado (banco ou env). " +
                "NFS-e de assinatura SaaS em modo mock indefinidamente. " +
                "Configure PLATFORM_FISCAL_CNPJ ou cadastre via /api/v1/admin/fiscal/platform.");
        } else {
            warnings.add("PlatformFiscalConfig usando fallback de variáveis de ambiente. " +
                "Prefira cadastrar os dados via /api/v1/admin/fiscal/platform para auditabilidade.");
        }

        // Erro fatal: Focus NFe de produção ativo sem CNPJ da plataforma
        BillingProperties.Nfe nfe = billing.nfe();
        if (nfe != null && nfe.isConfigured() && nfe.isProductionUrl() && !hasFallback) {
            errors.add("FOCUS_NFE_TOKEN de produção configurado mas CNPJ da RiseCode Studio ausente. " +
                "NFS-e seria emitida sem prestador válido.");
        }
    }

    public static class ProductionConfigException extends RuntimeException {
        public ProductionConfigException(String message) { super(message); }
    }
}
