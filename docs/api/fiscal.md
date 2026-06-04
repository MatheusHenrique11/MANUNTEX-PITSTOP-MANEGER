# API Fiscal — PitStop Manager

## Separação de Fluxos Fiscais

O sistema possui **dois fluxos fiscais completamente independentes**:

### Fluxo A — Assinatura SaaS (RiseCode → Oficina)

**Prestador:** RiseCode Studio (`platform_fiscal_config`)
**Tomador:** Oficina assinante (`empresas` + `tenant_fiscal_config`)
**Disparo:** Webhook Stripe `invoice.paid`
**Serviço:** `SaaSInvoiceService`
**InvoiceType:** `SAAS`

### Fluxo B — Serviço Automotivo (Oficina → Cliente Final)

**Prestador:** Oficina/tenant (`tenant_fiscal_config`)
**Tomador:** Cliente final da oficina (`clientes`)
**Disparo:** Conclusão de OS ou solicitação manual do gerente
**Serviço:** `WorkshopInvoiceService`
**InvoiceType:** `WORKSHOP`

> ⚠️ **Regra Fiscal Crítica:** O CNPJ da RiseCode Studio nunca deve aparecer como prestador
> nas notas emitidas pela oficina para clientes finais. Violação desta regra é fiscalmente ilegal.

---

## Endpoints de Configuração Fiscal

### Plataforma (ROLE_ADMIN)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/v1/admin/fiscal/platform` | Lê configuração fiscal da RiseCode Studio |
| PUT | `/api/v1/admin/fiscal/platform` | Atualiza configuração fiscal da RiseCode Studio |

### Tenant/Oficina

| Método | Endpoint | Roles | Descrição |
|--------|----------|-------|-----------|
| GET | `/api/v1/fiscal/tenant` | ADMIN, GERENTE | Lê config fiscal do próprio tenant |
| PUT | `/api/v1/fiscal/tenant` | ADMIN, GERENTE | Atualiza config fiscal do próprio tenant |
| GET | `/api/v1/admin/fiscal/tenant/{empresaId}` | ADMIN | Lê config fiscal de qualquer tenant |
| PUT | `/api/v1/admin/fiscal/tenant/{empresaId}` | ADMIN | Atualiza config fiscal de qualquer tenant |

---

## PlatformFiscalConfig — Campos Obrigatórios para Produção

| Campo | Obrigatoriedade |
|-------|----------------|
| `razaoSocial` | Obrigatório |
| `cnpj` | Obrigatório |
| `inscricaoMunicipal` | Obrigatório (a maioria dos municípios exige) |
| `codigoMunicipio` | Obrigatório (código IBGE) |
| `codigoServicoMunicipal` | Obrigatório (lista LC 116/2003) |
| `itemListaServico` | Obrigatório (ex: `1.01`) |
| `aliquotaIss` | Obrigatório |
| `ambienteFiscal` | Obrigatório (`HOMOLOGACAO` ou `PRODUCAO`) |

## TenantFiscalConfig — Campos para Habilitar Emissão

Para que `fiscalEnabled = true` seja válido, todos estes campos devem estar preenchidos:

- `cnpj`
- `inscricaoMunicipal`
- `codigoMunicipio`
- `codigoServicoMunicipal`
- `itemListaServico`

---

## Idempotência

Ambos os services verificam `gatewayInvoiceId` antes de emitir:

- `SaaSInvoiceService`: usa o `invoiceId` do Stripe (`inv_...`)
- `WorkshopInvoiceService`: usa a referência da OS (`os-<uuid>`)

Se já existe uma `FaturaNfe` com o mesmo `gatewayInvoiceId`, retorna a existente sem re-emitir.

---

## AuditLog

Toda alteração de `PlatformFiscalConfig` e `TenantFiscalConfig` gera um registro em `audit_logs`:

- `action`: `UPDATE`
- `resourceType`: `PLATFORM_FISCAL_CONFIG` ou `TENANT_FISCAL_CONFIG`
- `userEmail`: e-mail do usuário que realizou a alteração
- `detail`: mudanças realizadas e ambiente fiscal

Toda emissão de NFS-e gera um registro com:

- `action`: `NFS-E_EMITIDA`
- `resourceType`: `FATURA_NFE`
- `detail`: `gatewayInvoiceId`, valor, status
