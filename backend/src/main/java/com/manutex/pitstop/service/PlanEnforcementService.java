package com.manutex.pitstop.service;

import com.manutex.pitstop.config.AppFeatures;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.enums.PlanLimits;
import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.DocumentoRepository;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Ponto central de enforcement de limites por plano de assinatura.
 *
 * Todos os pontos de criação de OS, mecânicos e upload de documentos
 * devem chamar os métodos assert* antes de prosseguir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanEnforcementService {

    private final EmpresaRepository   empresaRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final DocumentoRepository  documentoRepository;
    private final UserRepository       userRepository;
    private final FeatureManager       featureManager;

    // ── Verificações de limite ────────────────────────────────────────────────

    public void assertCanCreateOS(UUID empresaId) {
        PlanLimits limits = limitsFor(empresaId);
        if (limits.isUnlimitedOs()) return;

        YearMonth now   = YearMonth.now();
        long count = manutencaoRepository.countByEmpresaAndMonth(
            empresaId, now.getYear(), now.getMonthValue());

        if (count >= limits.getMaxOsMensal()) {
            throw new PlanLimitExceededException(
                "Limite de %d ordens de serviço por mês atingido. Faça upgrade para continuar.".formatted(limits.getMaxOsMensal()));
        }
    }

    public void assertCanAddMechanic(UUID empresaId) {
        PlanLimits limits = limitsFor(empresaId);
        if (limits.isUnlimitedUsers()) return;

        long count = userRepository.countByEmpresaIdAndRole(empresaId, UserRole.ROLE_MECANICO);
        if (count >= limits.getMaxMecanicos()) {
            throw new PlanLimitExceededException(
                "Limite de %d mecânicos atingido. Faça upgrade para continuar.".formatted(limits.getMaxMecanicos()));
        }
    }

    public void assertCanUploadDocument(UUID empresaId, long fileSizeBytes) {
        PlanLimits limits = limitsFor(empresaId);
        if (limits.isUnlimitedStorage()) return;

        long used = documentoRepository.sumTamanhoBytesByEmpresaId(empresaId).orElse(0L);
        if (used + fileSizeBytes > limits.getMaxStorageBytes()) {
            long limitGb = limits.getMaxStorageBytes() / (1024 * 1024 * 1024);
            throw new PlanLimitExceededException(
                "Limite de %d GB de armazenamento atingido. Faça upgrade para continuar.".formatted(limitGb));
        }
    }

    // ── Ativação de features por plano ────────────────────────────────────────

    /**
     * Ativa exatamente as features do plano e desativa o restante.
     * Chamado após confirmação de pagamento via webhook do Stripe.
     */
    @Transactional
    public void activateFeaturesForPlan(Empresa empresa) {
        SubscriptionPlan plano = empresa.getSubscriptionPlan() != null
            ? empresa.getSubscriptionPlan()
            : SubscriptionPlan.STARTER;

        PlanLimits limits = PlanLimits.of(plano);

        for (AppFeatures feature : AppFeatures.values()) {
            boolean shouldBeActive = limits.getFeatures().contains(feature);
            featureManager.setFeatureState(new FeatureState(feature, shouldBeActive));
        }

        log.info("Features ativadas para empresa={} plano={}: {}",
            empresa.getId(), plano, limits.getFeatures());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PlanLimits limitsFor(UUID empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: " + empresaId));
        return PlanLimits.of(empresa.getSubscriptionPlan());
    }

    // ── Exceção ───────────────────────────────────────────────────────────────

    public static class PlanLimitExceededException extends RuntimeException {
        public PlanLimitExceededException(String message) { super(message); }
    }
}
