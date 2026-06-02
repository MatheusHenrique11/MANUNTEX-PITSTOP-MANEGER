package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;

import java.util.List;

/**
 * Snapshot de uso de recursos do tenant no período atual.
 * Usado pela página administrativa de controle de plano.
 */
public record PlanUsageResponse(
    SubscriptionPlan    plano,
    SubscriptionStatus  status,
    UsageMetric         os,
    UsageMetric         mecanicos,
    StorageMetric       storage,
    List<FeatureStatus> features
) {

    /** Métrica inteira com limite opcional (negativo = ilimitado). */
    public record UsageMetric(long used, long limit, boolean unlimited) {
        public static UsageMetric of(long used, int limit) {
            boolean unlimited = limit < 0;
            return new UsageMetric(used, unlimited ? 0 : limit, unlimited);
        }
    }

    /** Métrica de armazenamento em bytes. */
    public record StorageMetric(long usedBytes, long limitBytes, boolean unlimited) {
        public static StorageMetric of(long usedBytes, long limitBytes) {
            boolean unlimited = limitBytes < 0;
            return new StorageMetric(usedBytes, unlimited ? 0 : limitBytes, unlimited);
        }
    }

    /** Estado de uma feature flag em relação ao plano atual. */
    public record FeatureStatus(
        String  name,
        String  label,
        boolean active,
        boolean includedInPlan
    ) {}
}
