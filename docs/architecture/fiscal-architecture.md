# Arquitetura Fiscal — PitStop Manager

## Visão Geral

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FLUXO A — SAAS                               │
│                                                                     │
│  Stripe invoice.paid                                                │
│        │                                                            │
│        ▼                                                            │
│  WebhookController                                                  │
│        │                                                            │
│        ▼                                                            │
│  BillingService.processarPagamentoAprovado()                        │
│        │                                                            │
│        ▼                                                            │
│  SaaSInvoiceService.issueNfse()                                     │
│        │                                                            │
│        ├── PRESTADOR: PlatformFiscalConfig (RiseCode Studio)        │
│        │              CNPJ: configurado via /admin/fiscal/platform  │
│        │                                                            │
│        └── TOMADOR:   Empresa + TenantFiscalConfig (oficina)        │
│                                                                     │
│  Resultado: FaturaNfe { invoiceType = SAAS }                        │
│             AuditLog  { action = NFS-E_EMITIDA }                    │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      FLUXO B — WORKSHOP                             │
│                                                                     │
│  OS concluída / solicitação manual                                  │
│        │                                                            │
│        ▼                                                            │
│  WorkshopInvoiceService.issueWorkshopNfse()                         │
│        │                                                            │
│        ├── PRESTADOR: TenantFiscalConfig (oficina)                  │
│        │              CNPJ: configurado via /fiscal/tenant          │
│        │              fiscalEnabled deve ser true                   │
│        │                                                            │
│        └── TOMADOR:   Cliente final (nome + CPF/CNPJ)               │
│                                                                     │
│  Resultado: FaturaNfe { invoiceType = WORKSHOP }                    │
│             AuditLog  { action = NFS-E_EMITIDA }                    │
└─────────────────────────────────────────────────────────────────────┘
```

## Entidades

### PlatformFiscalConfig
- Tabela: `platform_fiscal_config` (linha única)
- Acesso: `ROLE_ADMIN` apenas
- Fallback: variáveis de ambiente `PLATFORM_FISCAL_*`
- Validação no startup: `ProductionReadinessValidator`

### TenantFiscalConfig
- Tabela: `tenant_fiscal_config` (1 por empresa)
- Acesso: `ROLE_GERENTE` (própria empresa), `ROLE_ADMIN` (qualquer)
- `fiscalEnabled`: deve ser `true` para emissão via Fluxo B

### FaturaNfe
- Tabela: `faturas_nfe`
- Campo `invoice_type`: `SAAS` ou `WORKSHOP`
- Campo `gateway_invoice_id`: `UNIQUE` (garante idempotência no banco)

## Segurança

| Recurso | ROLE_ADMIN | ROLE_GERENTE | ROLE_MECANICO |
|---------|-----------|--------------|---------------|
| PlatformFiscalConfig | Leitura + Escrita | — | — |
| TenantFiscalConfig (própria) | Leitura + Escrita | Leitura + Escrita | — |
| TenantFiscalConfig (qualquer) | Leitura + Escrita | — | — |
| Histórico de NFS-e | Leitura | Leitura | — |

## ProductionReadinessValidator

Executado em `ApplicationReadyEvent`. Regras:

**Avisos (WARN):**
- `STRIPE_SECRET_KEY` ausente
- `STRIPE_SECRET_KEY` é chave de teste (`sk_test_`)
- `STRIPE_WEBHOOK_SECRET` ausente
- Price IDs dos planos ausentes
- `FOCUS_NFE_TOKEN` ausente
- `PlatformFiscalConfig` incompleta

**Erros Fatais (lança exceção, impede startup):**
- Chave Stripe real (`sk_live_`) sem `STRIPE_WEBHOOK_SECRET`
- Focus NFe de produção ativo + ambiente `PRODUCAO` + dados fiscais incompletos
- Focus NFe de produção ativo sem CNPJ da RiseCode Studio
