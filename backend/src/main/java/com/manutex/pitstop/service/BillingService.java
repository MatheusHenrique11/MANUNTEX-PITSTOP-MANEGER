package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Assinatura;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;
import com.manutex.pitstop.domain.repository.AssinaturaRepository;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.FaturaNfeRepository;
import com.manutex.pitstop.web.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final EmpresaRepository empresaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final FaturaNfeRepository faturaNfeRepository;
    private final PaymentGatewayService paymentGateway;
    private final TaxInvoiceService taxInvoiceService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AssinaturaResponse getAssinatura(UUID empresaId) {
        Assinatura assinatura = assinaturaRepository
            .findTopByEmpresaIdOrderByCreatedAtDesc(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Assinatura não encontrada para empresa: " + empresaId));
        return toAssinaturaResponse(assinatura);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatus getStatus(UUID empresaId) {
        Empresa empresa = loadEmpresa(empresaId);
        return empresa.getSubscriptionStatus();
    }

    @Transactional(readOnly = true)
    public List<FaturaNfeResponse> getFaturas(UUID empresaId) {
        return faturaNfeRepository.findByEmpresaIdOrderByIssueDateDesc(empresaId)
            .stream()
            .map(this::toFaturaNfeResponse)
            .toList();
    }

    // ── Ações ────────────────────────────────────────────────────────────────

    @Transactional
    public CheckoutResponse criarCheckout(UUID empresaId, SubscriptionPlan plano) {
        Empresa empresa = loadEmpresa(empresaId);

        String customerId = empresa.getPaymentGatewayCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = paymentGateway.createCustomer(empresa);
            empresa.setPaymentGatewayCustomerId(customerId);
            empresaRepository.save(empresa);
        }

        String successUrl = frontendUrl + "/billing/dashboard?success=true";
        String cancelUrl  = frontendUrl + "/billing/pricing?canceled=true";

        String checkoutUrl = paymentGateway.createCheckoutSession(customerId, plano, successUrl, cancelUrl);
        log.info("Checkout criado para empresa={} plano={}", empresaId, plano);
        return new CheckoutResponse(checkoutUrl);
    }

    @Transactional
    public void cancelarAssinatura(UUID empresaId) {
        Empresa empresa = loadEmpresa(empresaId);

        if (empresa.getSubscriptionId() == null) {
            throw new SubscriptionException("Empresa não possui assinatura ativa para cancelar.");
        }

        paymentGateway.cancelSubscription(empresa.getSubscriptionId());

        empresa.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        empresaRepository.save(empresa);

        assinaturaRepository.findTopByEmpresaIdOrderByCreatedAtDesc(empresaId).ifPresent(a -> {
            a.setStatus(SubscriptionStatus.CANCELED);
            assinaturaRepository.save(a);
        });

        log.info("Assinatura cancelada para empresa={}", empresaId);
    }

    // ── Processamento de Webhooks ─────────────────────────────────────────────

    @Transactional
    public void processarPagamentoAprovado(StripeWebhookEvent event) {
        StripeWebhookEvent.EventObject obj = event.data().object();
        String customerId  = obj.customer();
        String gatewayInvoiceId = obj.id();
        BigDecimal amount  = obj.amountPaidAsBrl();

        Empresa empresa = empresaRepository.findByPaymentGatewayCustomerId(customerId)
            .orElseGet(() -> {
                log.warn("Webhook invoice.paid: customerId={} não encontrado, ignorando", customerId);
                return null;
            });
        if (empresa == null) return;

        empresa.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (obj.subscription() != null) {
            empresa.setSubscriptionId(obj.subscription());
        }
        empresaRepository.save(empresa);

        atualizarOuCriarAssinatura(empresa, obj.subscription(), SubscriptionStatus.ACTIVE);

        taxInvoiceService.issueNfse(empresa, amount, gatewayInvoiceId);
        log.info("Pagamento aprovado processado: empresa={} valor={}", empresa.getId(), amount);
    }

    @Transactional
    public void processarAssinaturaCancelada(StripeWebhookEvent event) {
        String customerId = event.data().object().customer();
        empresaRepository.findByPaymentGatewayCustomerId(customerId).ifPresent(empresa -> {
            empresa.setSubscriptionStatus(SubscriptionStatus.CANCELED);
            empresaRepository.save(empresa);
            atualizarOuCriarAssinatura(empresa, event.data().object().subscription(), SubscriptionStatus.CANCELED);
            log.info("Assinatura cancelada via webhook para empresa={}", empresa.getId());
        });
    }

    @Transactional
    public void processarPagamentoFalhou(StripeWebhookEvent event) {
        String customerId = event.data().object().customer();
        empresaRepository.findByPaymentGatewayCustomerId(customerId).ifPresent(empresa -> {
            empresa.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
            empresaRepository.save(empresa);
            log.warn("Pagamento falhou para empresa={} — status alterado para PAST_DUE", empresa.getId());
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Empresa loadEmpresa(UUID empresaId) {
        return empresaRepository.findById(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: " + empresaId));
    }

    @SuppressWarnings("null") // empresa.getId() não é null — garantido pelo loadEmpresa; falso positivo do Eclipse
    private void atualizarOuCriarAssinatura(Empresa empresa, String subscriptionId, SubscriptionStatus status) {
        Assinatura assinatura = assinaturaRepository
            .findTopByEmpresaIdOrderByCreatedAtDesc(empresa.getId())
            .orElseGet(() -> Assinatura.builder()
                .empresa(empresa)
                .plano(empresa.getSubscriptionPlan() != null
                    ? empresa.getSubscriptionPlan() : SubscriptionPlan.STARTER)
                .build());

        assinatura.setStatus(status);
        if (subscriptionId != null) {
            assinatura.setGatewaySubscriptionId(subscriptionId);
        }
        assinaturaRepository.save(assinatura);
    }

    private AssinaturaResponse toAssinaturaResponse(Assinatura a) {
        return new AssinaturaResponse(
            a.getId(), a.getPlano(), a.getStatus(),
            a.getGatewaySubscriptionId(),
            a.getCurrentPeriodStart(), a.getCurrentPeriodEnd(),
            a.getTrialEnd(), a.getCreatedAt()
        );
    }

    private FaturaNfeResponse toFaturaNfeResponse(com.manutex.pitstop.domain.entity.FaturaNfe f) {
        return new FaturaNfeResponse(
            f.getId(), f.getGatewayInvoiceId(), f.getNfeId(), f.getNfeStatus(),
            f.getValor(), f.getPdfUrl(), f.getXmlUrl(), f.getIssueDate()
        );
    }

    public static class SubscriptionException extends RuntimeException {
        public SubscriptionException(String message) { super(message); }
    }

    public static class SubscriptionRequiredException extends RuntimeException {
        public SubscriptionRequiredException(String message) { super(message); }
    }
}
