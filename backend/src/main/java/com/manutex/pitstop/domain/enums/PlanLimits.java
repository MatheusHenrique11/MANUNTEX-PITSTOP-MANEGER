package com.manutex.pitstop.domain.enums;

import com.manutex.pitstop.config.AppFeatures;

import java.util.Set;

/**
 * Limites quantitativos e features liberadas por plano de assinatura.
 *
 * Valores negativos (-1) indicam recurso ilimitado.
 * Alterar os limites aqui propaga automaticamente para todos os pontos de enforcement.
 */
public enum PlanLimits {

    STARTER(
        50,                     // max OS por mês
        2,                      // max mecânicos ativos
        5L * 1024 * 1024 * 1024, // 5 GB em bytes
        Set.of(
            AppFeatures.VEHICLE_MANAGEMENT,
            AppFeatures.DOCUMENT_VAULT,
            AppFeatures.MAINTENANCE_MODULE
        )
    ),

    PROFESSIONAL(
        -1,                      // ilimitado
        -1,                      // ilimitado
        50L * 1024 * 1024 * 1024, // 50 GB em bytes
        Set.of(
            AppFeatures.VEHICLE_MANAGEMENT,
            AppFeatures.DOCUMENT_VAULT,
            AppFeatures.MAINTENANCE_MODULE,
            AppFeatures.GOALS_MODULE,
            AppFeatures.FINANCIAL_MODULE,
            AppFeatures.ANALYTICS_DASHBOARD,
            AppFeatures.NOTIFICATIONS
        )
    ),

    ENTERPRISE(
        -1,  // ilimitado
        -1,  // ilimitado
        -1L, // ilimitado
        Set.of(AppFeatures.values())
    );

    private final int maxOsMensal;
    private final int maxMecanicos;
    private final long maxStorageBytes;
    private final Set<AppFeatures> features;

    PlanLimits(int maxOsMensal, int maxMecanicos, long maxStorageBytes, Set<AppFeatures> features) {
        this.maxOsMensal    = maxOsMensal;
        this.maxMecanicos   = maxMecanicos;
        this.maxStorageBytes = maxStorageBytes;
        this.features        = features;
    }

    public int  getMaxOsMensal()     { return maxOsMensal; }
    public int  getMaxMecanicos()    { return maxMecanicos; }
    public long getMaxStorageBytes() { return maxStorageBytes; }
    public Set<AppFeatures> getFeatures() { return features; }

    public boolean isUnlimitedOs()      { return maxOsMensal < 0; }
    public boolean isUnlimitedUsers()   { return maxMecanicos < 0; }
    public boolean isUnlimitedStorage() { return maxStorageBytes < 0; }

    /** Retorna os limites correspondentes ao plano. Null/TRIAL trata como STARTER. */
    public static PlanLimits of(SubscriptionPlan plan) {
        if (plan == null) return STARTER;
        return switch (plan) {
            case STARTER      -> STARTER;
            case PROFESSIONAL -> PROFESSIONAL;
            case ENTERPRISE   -> ENTERPRISE;
        };
    }
}
