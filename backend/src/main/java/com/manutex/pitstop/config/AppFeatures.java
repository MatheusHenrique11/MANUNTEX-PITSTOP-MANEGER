package com.manutex.pitstop.config;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Enum central de Feature Flags do sistema.
 * Adicione novas features aqui — elas aparecerão automaticamente no painel Togglz
 * e no endpoint /api/v1/admin/features do Angular.
 */
public enum AppFeatures implements Feature {

    @EnabledByDefault
    @Label("Módulo de Gestão de Veículos")
    VEHICLE_MANAGEMENT,

    @EnabledByDefault
    @Label("Cofre de Documentos (Upload PDF)")
    DOCUMENT_VAULT,

    @Label("Módulo de Manutenções")
    MAINTENANCE_MODULE,

    @Label("Relatórios e Dashboard Analítico")
    ANALYTICS_DASHBOARD,

    @Label("Notificações por E-mail / WhatsApp")
    NOTIFICATIONS,

    @Label("Módulo Financeiro (Orçamentos e Cobranças)")
    FINANCIAL_MODULE,

    @Label("Integração com DETRAN (validação online)")
    DETRAN_INTEGRATION;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
