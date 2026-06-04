package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.*;
import com.manutex.pitstop.domain.enums.ConsentType;
import com.manutex.pitstop.domain.enums.DsarStatus;
import com.manutex.pitstop.domain.repository.*;
import com.manutex.pitstop.domain.repository.NotificationLogRepository;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.web.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de conformidade LGPD (Lei 13.709/2018).
 *
 * Implementa:
 *   Art. 8  — Consentimento e revogação
 *   Art. 18 — Direitos do titular (acesso, portabilidade, correção, anonimização, oposição)
 *   Art. 46 — Medidas de segurança e auditoria
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LgpdService {

    // Versões atuais das políticas — ao incrementar, usuários precisarão re-consentir
    public static final String PRIVACY_POLICY_VERSION = "1.0";
    public static final String TERMS_OF_USE_VERSION   = "1.0";

    // Prazo de resposta às solicitações do titular (Art. 18 §4 — prazo razoável)
    private static final int DSAR_DEADLINE_DAYS = 15;

    private final UserRepository               userRepository;
    private final UserConsentRepository        userConsentRepository;
    private final DataSubjectRequestRepository dsarRepository;
    private final AuditLogRepository           auditLogRepository;
    private final RefreshTokenRepository       refreshTokenRepository;
    private final EmpresaRepository            empresaRepository;
    private final PasswordEncoder              passwordEncoder;
    private final NotificationLogRepository    notificationLogRepository;

    // ── Consentimento ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ConsentStatusResponse getConsentStatus(UUID userId) {
        var ppConsent = userConsentRepository
            .findTopByUserIdAndPolicyTypeAndAcceptedTrueOrderByCreatedAtDesc(
                userId, ConsentType.PRIVACY_POLICY);
        var touConsent = userConsentRepository
            .findTopByUserIdAndPolicyTypeAndAcceptedTrueOrderByCreatedAtDesc(
                userId, ConsentType.TERMS_OF_USE);

        boolean ppOk  = ppConsent.map(c -> PRIVACY_POLICY_VERSION.equals(c.getPolicyVersion())).orElse(false);
        boolean touOk = touConsent.map(c -> TERMS_OF_USE_VERSION.equals(c.getPolicyVersion())).orElse(false);

        return new ConsentStatusResponse(
            PRIVACY_POLICY_VERSION, ppOk,  ppConsent.map(UserConsent::getCreatedAt).orElse(null),
            TERMS_OF_USE_VERSION,   touOk, touConsent.map(UserConsent::getCreatedAt).orElse(null),
            ppOk && touOk
        );
    }

    @Transactional
    @SuppressWarnings("null")
    public ConsentStatusResponse recordConsent(UUID userId, ConsentRequest req, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        userConsentRepository.save(
            UserConsent.builder()
                .user(user)
                .policyType(req.policyType())
                .policyVersion(req.policyVersion())
                .accepted(req.accepted())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build()
        );

        auditLogRepository.save(AuditLog.builder()
            .empresaId(user.getEmpresa() != null ? user.getEmpresa().getId() : null)
            .userEmail(user.getEmail())
            .action("CONSENT")
            .resourceType("USER")
            .resourceId(userId.toString())
            .detail(req.policyType() + " v" + req.policyVersion() + " → " + (req.accepted() ? "ACEITO" : "RECUSADO"))
            .ipAddress(ipAddress)
            .build());

        log.info("Consentimento registrado: user={} tipo={} versão={} aceito={}",
            userId, req.policyType(), req.policyVersion(), req.accepted());

        return getConsentStatus(userId);
    }

    // ── Solicitações do Titular (DSARs) ───────────────────────────────────────

    @Transactional
    @SuppressWarnings("null")
    public DsarResponse submitDsar(UUID userId, DsarRequest req, String ipAddress) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        UUID empresaId = TenantContext.requireEmpresaId();
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));

        DataSubjectRequest dsar = dsarRepository.save(
            DataSubjectRequest.builder()
                .empresa(empresa)
                .requesterUserId(userId)
                .requesterEmail(user.getEmail())
                .requestType(req.requestType())
                .status(DsarStatus.PENDING)
                .notes(req.notes())
                .deadlineAt(Instant.now().plus(DSAR_DEADLINE_DAYS, ChronoUnit.DAYS))
                .build()
        );

        auditLogRepository.save(AuditLog.builder()
            .empresaId(empresaId)
            .userEmail(user.getEmail())
            .action("DSAR_SUBMITTED")
            .resourceType("DSAR")
            .resourceId(dsar.getId().toString())
            .detail("Tipo: " + req.requestType())
            .ipAddress(ipAddress)
            .build());

        log.info("DSAR criada: id={} user={} tipo={}", dsar.getId(), userId, req.requestType());
        return DsarResponse.of(dsar);
    }

    @Transactional(readOnly = true)
    public List<DsarResponse> listUserDsars(UUID userId) {
        return dsarRepository.findByRequesterUserIdOrderByRequestedAtDesc(userId)
            .stream().map(DsarResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<DsarResponse> listAllDsars(UUID empresaId) {
        return dsarRepository.findByEmpresaIdOrderByRequestedAtDesc(empresaId)
            .stream().map(DsarResponse::of).toList();
    }

    @Transactional
    @SuppressWarnings("null")
    public DsarResponse completeDsar(UUID dsarId, String responseNotes, Authentication auth) {
        DataSubjectRequest dsar = dsarRepository.findById(dsarId)
            .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada: " + dsarId));

        dsar.setStatus(DsarStatus.COMPLETED);
        dsar.setResponseNotes(responseNotes);
        dsar.setCompletedAt(Instant.now());
        dsar.setProcessedBy(auth.getName());

        auditLogRepository.save(AuditLog.builder()
            .empresaId(dsar.getEmpresa().getId())
            .userEmail(auth.getName())
            .action("DSAR_COMPLETED")
            .resourceType("DSAR")
            .resourceId(dsarId.toString())
            .detail("Tipo: " + dsar.getRequestType() + " | Resp: " + responseNotes)
            .build());

        return DsarResponse.of(dsarRepository.save(dsar));
    }

    // ── Portabilidade de Dados (Art. 18, II-III LGPD) ─────────────────────────

    @Transactional(readOnly = true)
    @SuppressWarnings("null") // JpaRepository returns are @NonNull by contract; falso positivo do Eclipse
    public MyDataExport exportMyData(UUID userId, Authentication auth, String ipAddress) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        var consents = userConsentRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(c -> new MyDataExport.ConsentRecord(c.getPolicyType(), c.getPolicyVersion(), c.isAccepted(), c.getCreatedAt()))
            .toList();

        var dsars = dsarRepository.findByRequesterUserIdOrderByRequestedAtDesc(userId)
            .stream()
            .map(d -> new MyDataExport.DsarSummary(d.getId(), d.getRequestType(), d.getStatus(), d.getRequestedAt(), d.getDeadlineAt(), d.getCompletedAt()))
            .toList();

        auditLogRepository.save(AuditLog.builder()
            .empresaId(user.getEmpresa() != null ? user.getEmpresa().getId() : null)
            .userEmail(user.getEmail())
            .action("DATA_EXPORT")
            .resourceType("USER")
            .resourceId(userId.toString())
            .detail("Exportação de dados pessoais (portabilidade)")
            .ipAddress(ipAddress)
            .build());

        log.info("Exportação de dados: user={}", userId);

        return new MyDataExport(
            Instant.now(),
            new MyDataExport.UserData(
                user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(),
                user.getEmpresa() != null ? user.getEmpresa().getNome() : null,
                user.getCreatedAt()
            ),
            consents,
            dsars
        );
    }

    // ── Anonimização / Direito ao Esquecimento (Art. 18, IV-VI LGPD) ─────────

    @Transactional
    @SuppressWarnings("null")
    public void anonymizeUser(UUID userId, Authentication auth) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        String anonEmail = "anon-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                           + "@pitstop.anonimizado";

        UUID empresaId = user.getEmpresa() != null ? user.getEmpresa().getId() : null;
        String originalEmail = user.getEmail();

        // Anonimiza dados pessoais
        user.setEmail(anonEmail);
        user.setFullName("Usuário Anonimizado");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());

        // Revoga todos os tokens de sessão
        refreshTokenRepository.revokeAllByUserId(userId);

        // Nota: NotificationLog.clienteId referencia Cliente (cliente da oficina), não User.
        // A anonimização de User (mecânicos, gerentes) não tem clienteId correspondente.
        // A limpeza de logs por clienteId ocorre em softDeleteCliente(), não aqui.

        userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
            .empresaId(empresaId)
            .userEmail(auth.getName())
            .action("ANONYMIZE")
            .resourceType("USER")
            .resourceId(userId.toString())
            .detail("Dados pessoais anonimizados. E-mail original: " + originalEmail)
            .build());

        log.info("Usuário anonimizado: id={} por={}", userId, auth.getName());
    }

    @Transactional
    @SuppressWarnings("null")
    public void softDeleteCliente(UUID clienteId, Authentication auth, UUID empresaId) {
        // LGPD: apaga logs de notificação com telefone/e-mail do cliente sendo removido
        notificationLogRepository.deleteByClienteId(clienteId);

        auditLogRepository.save(AuditLog.builder()
            .empresaId(empresaId)
            .userEmail(auth.getName())
            .action("SOFT_DELETE")
            .resourceType("CLIENTE")
            .resourceId(clienteId.toString())
            .detail("Exclusão lógica via LGPD + notification_logs removidos")
            .build());
    }

    // ── Exceções ──────────────────────────────────────────────────────────────

    public static class ConsentRequiredException extends RuntimeException {
        public ConsentRequiredException(String msg) { super(msg); }
    }
}
