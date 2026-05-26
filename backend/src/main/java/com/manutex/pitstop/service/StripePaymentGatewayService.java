package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementação mock do PaymentGatewayService usando Stripe.
 *
 * Em produção, substitua os métodos abaixo pela SDK oficial do Stripe:
 *   implementation 'com.stripe:stripe-java:25.x.x'
 *
 * Os tokens são lidos de variáveis de ambiente (STRIPE_SECRET_KEY).
 * Enquanto não configurados, os métodos retornam IDs simulados para
 * permitir o desenvolvimento e os testes locais.
 */
@Slf4j
@Service
public class StripePaymentGatewayService implements PaymentGatewayService {

    @Value("${billing.stripe.secret-key:}")
    private String secretKey;

    @Value("${billing.stripe.webhook-secret:}")
    private String webhookSecret;

    @Override
    public String createCustomer(Empresa empresa) {
        if (secretKey.isBlank()) {
            String mockId = "cus_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[STRIPE-MOCK] createCustomer para empresa={} → {}", empresa.getId(), mockId);
            return mockId;
        }
        // TODO: integração real
        // CustomerCreateParams params = CustomerCreateParams.builder()
        //     .setEmail(empresa.getEmail())
        //     .setName(empresa.getNome())
        //     .setMetadata(Map.of("empresa_id", empresa.getId().toString()))
        //     .build();
        // return Customer.create(params).getId();
        throw new UnsupportedOperationException("Configure STRIPE_SECRET_KEY para usar o gateway real.");
    }

    @Override
    public String createSubscription(String customerId, SubscriptionPlan plan) {
        if (secretKey.isBlank()) {
            String mockId = "sub_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[STRIPE-MOCK] createSubscription customer={} plano={} → {}", customerId, plan, mockId);
            return mockId;
        }
        // TODO: integração real
        // SubscriptionCreateParams params = SubscriptionCreateParams.builder()
        //     .setCustomer(customerId)
        //     .addItem(SubscriptionCreateParams.Item.builder()
        //         .setPrice(PRICE_IDS.get(plan))
        //         .build())
        //     .build();
        // return Subscription.create(params).getId();
        throw new UnsupportedOperationException("Configure STRIPE_SECRET_KEY para usar o gateway real.");
    }

    @Override
    public String createCheckoutSession(String customerId, SubscriptionPlan plan,
                                        String successUrl, String cancelUrl) {
        if (secretKey.isBlank()) {
            String mockUrl = successUrl + "?session_id=cs_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[STRIPE-MOCK] checkoutSession plano={} → {}", plan, mockUrl);
            return mockUrl;
        }
        // TODO: integração real
        // SessionCreateParams params = SessionCreateParams.builder()
        //     .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        //     .setCustomer(customerId)
        //     .addLineItem(SessionCreateParams.LineItem.builder()
        //         .setPrice(PRICE_IDS.get(plan))
        //         .setQuantity(1L)
        //         .build())
        //     .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
        //     .setCancelUrl(cancelUrl)
        //     .build();
        // return Session.create(params).getUrl();
        throw new UnsupportedOperationException("Configure STRIPE_SECRET_KEY para usar o gateway real.");
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        if (secretKey.isBlank()) {
            log.info("[STRIPE-MOCK] cancelSubscription sub={}", subscriptionId);
            return;
        }
        // TODO: integração real
        // Subscription.retrieve(subscriptionId).cancel();
        throw new UnsupportedOperationException("Configure STRIPE_SECRET_KEY para usar o gateway real.");
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
