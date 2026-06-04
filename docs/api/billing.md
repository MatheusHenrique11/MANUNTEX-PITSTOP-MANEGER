# API de Billing — PitStop Manager

## Endpoints

### Assinatura e Plano

| Método | Endpoint | Roles | Descrição |
|--------|----------|-------|-----------|
| GET | `/api/v1/billing/subscription` | ADMIN, GERENTE | Retorna a assinatura atual do tenant |
| GET | `/api/v1/billing/subscription/status` | Autenticado | Status da assinatura (usado por guards) |
| GET | `/api/v1/billing/plan-usage` | ADMIN, GERENTE | Uso de recursos e features do plano |
| GET | `/api/v1/billing/invoices` | ADMIN, GERENTE | Histórico de NFS-e e faturas |
| POST | `/api/v1/billing/checkout` | ADMIN, GERENTE | Cria sessão de checkout no Stripe |
| DELETE | `/api/v1/billing/subscription` | ADMIN, GERENTE | Cancela a assinatura |

### Webhook Stripe

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| POST | `/api/v1/webhooks/stripe` | Nenhuma (validado por HMAC) | Recebe eventos do Stripe |

**Eventos processados:**
- `invoice.paid` → ativa assinatura + emite NFS-e SaaS (RiseCode → oficina)
- `invoice.payment_failed` → altera status para `PAST_DUE`
- `customer.subscription.deleted` → cancela assinatura

**Segurança:** o webhook valida `Stripe-Signature` via HMAC-SHA256 com `STRIPE_WEBHOOK_SECRET`.
Se o secret não estiver configurado, aceita em modo dev com aviso no log.

## Variáveis de Ambiente Necessárias

```
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRICE_STARTER=price_...
STRIPE_PRICE_PROFESSIONAL=price_...
STRIPE_PRICE_ENTERPRISE=price_...
```

## Fluxo de Checkout

1. Frontend chama `POST /api/v1/billing/checkout` com o plano escolhido
2. Backend cria/recupera o Customer no Stripe e gera uma Checkout Session
3. Frontend redireciona o usuário para a URL retornada
4. Após pagamento, Stripe dispara `invoice.paid` para o webhook
5. Backend ativa a assinatura, ativa feature flags e emite NFS-e via SaaSInvoiceService
