package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.enums.SubscriptionPlan;

public interface PaymentGatewayService {

    /** Cria o cliente no gateway de pagamento e retorna o customerId externo. */
    String createCustomer(Empresa empresa);

    /** Inicia uma assinatura no gateway e retorna o subscriptionId externo. */
    String createSubscription(String customerId, SubscriptionPlan plan);

    /**
     * Gera uma URL de checkout hospedada no gateway (ex: Stripe Checkout).
     * O usuário é redirecionado para esta URL para inserir os dados de pagamento.
     */
    String createCheckoutSession(String customerId, SubscriptionPlan plan,
                                 String successUrl, String cancelUrl);

    /** Cancela a assinatura no gateway ao final do período vigente. */
    void cancelSubscription(String subscriptionId);
}
