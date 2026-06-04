# Arquitetura de Billing — PitStop Manager

## Stack

- **Gateway de Pagamento:** Stripe (Checkout Sessions + Webhooks)
- **Emissão Fiscal:** Focus NFe API v2 (NFS-e)
- **Feature Flags:** Togglz (ativação automática por plano)

## Fluxo de Assinatura

```
1. Usuário escolhe plano em /billing/pricing
2. POST /api/v1/billing/checkout { plano }
3. Backend cria Customer no Stripe (se necessário)
4. Backend retorna URL da Checkout Session
5. Frontend redireciona para URL do Stripe
6. Usuário insere dados de pagamento
7. Stripe redireciona de volta com ?success=true
8. Stripe envia webhook invoice.paid (assíncrono)
9. Backend:
   a. Ativa SubscriptionStatus = ACTIVE
   b. Ativa feature flags do plano
   c. Emite NFS-e via SaaSInvoiceService (RiseCode → oficina)
   d. Registra AuditLog
```

## Estados da Assinatura

| Status | Significado | Acesso |
|--------|-------------|--------|
| `TRIAL` | Período de avaliação | Liberado |
| `ACTIVE` | Assinatura ativa e em dia | Liberado |
| `PAST_DUE` | Pagamento atrasado | Bloqueado pelo subscriptionGuard |
| `CANCELED` | Cancelada pelo usuário | Bloqueado |
| `SUSPENDED` | Suspensa pela plataforma | Bloqueado |

## Idempotência de Webhooks

O campo `gateway_invoice_id` na tabela `faturas_nfe` tem constraint `UNIQUE`.
`SaaSInvoiceService` verifica `existsByGatewayInvoiceId` antes de emitir.
Webhooks repetidos do mesmo `invoice.paid` são ignorados sem erro.

## Planos e Price IDs

Os Price IDs são criados no Stripe Dashboard e configurados via variáveis de ambiente:

```
STRIPE_PRICE_STARTER=price_...
STRIPE_PRICE_PROFESSIONAL=price_...
STRIPE_PRICE_ENTERPRISE=price_...
```

A implementação real do `createCheckoutSession` (Stripe SDK) deve ler esses IDs via `BillingProperties.stripe()`.

## Segurança do Webhook

A assinatura do Stripe-Signature header é validada via HMAC-SHA256:

```
signedPayload = timestamp + "." + payload
expectedHash  = HMAC-SHA256(webhookSecret, signedPayload)
```

Em produção, `STRIPE_WEBHOOK_SECRET` é obrigatório. O `ProductionReadinessValidator`
lança erro fatal se a chave de produção do Stripe estiver configurada mas o webhook secret estiver ausente.
