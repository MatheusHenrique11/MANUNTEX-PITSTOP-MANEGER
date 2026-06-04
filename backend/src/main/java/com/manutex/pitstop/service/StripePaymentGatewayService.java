package com.manutex.pitstop.service;

import com.manutex.pitstop.config.BillingProperties;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Integração com a Stripe Java SDK (com.stripe:stripe-java).
 *
 * Modo mock: quando STRIPE_SECRET_KEY não está configurada, todos os métodos
 * retornam IDs simulados para não bloquear o desenvolvimento local.
 *
 * Modo real: quando STRIPE_SECRET_KEY está configurada (sk_live_ ou sk_test_),
 * os métodos chamam a API da Stripe via SDK oficial.
 *
 * Price IDs são lidos de BillingProperties (variáveis STRIPE_PRICE_*).
 */
@Slf4j
@Service
public class StripePaymentGatewayService implements PaymentGatewayService {

    private final BillingProperties billingProperties;

    @Value("${billing.stripe.secret-key:}")
    private String secretKey;

    @Value("${billing.stripe.webhook-secret:}")
    private String webhookSecret;

    public StripePaymentGatewayService(BillingProperties billingProperties) {
        this.billingProperties = billingProperties;
    }

    // ── PaymentGatewayService ─────────────────────────────────────────────────

    @Override
    public String createCustomer(Empresa empresa) {
        if (secretKey.isBlank()) {
            String mockId = "cus_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[STRIPE-MOCK] createCustomer empresa={} → {}", empresa.getId(), mockId);
            return mockId;
        }

        initStripe();
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(empresa.getNome())
                .putMetadata("empresa_id", empresa.getId().toString())
                .build();

            Customer customer = Customer.create(params);
            log.info("Stripe Customer criado: customerId={} empresa={}", customer.getId(), empresa.getId());
            return customer.getId();

        } catch (StripeException e) {
            log.error("Erro ao criar Customer no Stripe para empresa={}: {}", empresa.getId(), e.getMessage());
            throw new StripeIntegrationException("Falha ao criar cliente no Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public String createSubscription(String customerId, SubscriptionPlan plan) {
        if (secretKey.isBlank()) {
            String mockId = "sub_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[STRIPE-MOCK] createSubscription customer={} plano={} → {}", customerId, plan, mockId);
            return mockId;
        }
        // Subscriptions são criadas via Checkout Session no fluxo atual.
        // Este método permanece disponível para criação programática se necessário.
        throw new UnsupportedOperationException(
            "Use createCheckoutSession para iniciar assinaturas via Stripe Checkout.");
    }

    @Override
    public String createCheckoutSession(String customerId, SubscriptionPlan plan,
                                        String successUrl, String cancelUrl) {
        if (secretKey.isBlank()) {
            String mockUrl = successUrl + "?session_id=cs_mock_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[STRIPE-MOCK] checkoutSession plano={} → {}", plan, mockUrl);
            return mockUrl;
        }

        String priceId = resolvePriceId(plan);
        initStripe();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                )
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .putMetadata("plano", plan.name())
                .build();

            Session session = Session.create(params);
            log.info("Stripe Checkout Session criada: sessionId={} plano={}", session.getId(), plan);
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Erro ao criar Checkout Session no Stripe: plano={} erro={}", plan, e.getMessage());
            throw new StripeIntegrationException("Falha ao criar sessão de checkout: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        if (secretKey.isBlank()) {
            log.info("[STRIPE-MOCK] cancelSubscription sub={}", subscriptionId);
            return;
        }

        initStripe();
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            subscription.cancel();
            log.info("Stripe Subscription cancelada: subscriptionId={}", subscriptionId);

        } catch (StripeException e) {
            log.error("Erro ao cancelar Subscription no Stripe: sub={} erro={}", subscriptionId, e.getMessage());
            throw new StripeIntegrationException("Falha ao cancelar assinatura: " + e.getMessage(), e);
        }
    }

    // ── Acesso ao webhook secret (usado pelo WebhookController) ───────────────

    public String getWebhookSecret() {
        return webhookSecret;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Inicializa a API key da Stripe antes de cada chamada. Thread-safe: é idempotente. */
    private void initStripe() {
        Stripe.apiKey = secretKey;
    }

    private String resolvePriceId(SubscriptionPlan plan) {
        BillingProperties.Stripe stripe = billingProperties.stripe();
        String priceId = switch (plan) {
            case STARTER      -> stripe != null ? stripe.priceStarter()      : null;
            case PROFESSIONAL -> stripe != null ? stripe.priceProfessional() : null;
            case ENTERPRISE   -> stripe != null ? stripe.priceEnterprise()   : null;
        };

        if (priceId == null || priceId.isBlank()) {
            throw new StripeIntegrationException(
                "Price ID não configurado para o plano " + plan.name() +
                ". Configure STRIPE_PRICE_" + plan.name() + " nas variáveis de ambiente.");
        }
        return priceId;
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    public static class StripeIntegrationException extends RuntimeException {
        public StripeIntegrationException(String message, Throwable cause) { super(message, cause); }
        public StripeIntegrationException(String message) { super(message); }
    }
}
