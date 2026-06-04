# PitStop Manager — Roadmap Arquitetural Enterprise

**Versão:** 1.0  
**Data:** Junho 2025  
**Responsável:** RiseCode Studio — Arquitetura Principal  
**Status:** Plano aprovado para implementação faseada

---

## Inventário da Arquitetura Atual

Antes de qualquer planejamento, foi realizado levantamento completo do estado presente.

### Entidades de Domínio (15)

| Entidade | Tenant-scoped | Soft-delete | Auditoria |
|---|:---:|:---:|:---:|
| `User` | ✅ `empresa_id` | ✅ `deleted_at` | ✅ |
| `Empresa` | — (tenant raiz) | ❌ | ✅ |
| `Cliente` | ✅ `empresa_id` | ✅ `deleted_at` | ✅ |
| `Veiculo` | ✅ via `cliente` | ❌ | ✅ |
| `Manutencao` | ✅ via `mecanico` | ❌ | ✅ |
| `Documento` | ✅ via `uploadedBy` | ❌ | ✅ |
| `Assinatura` | ✅ `empresa_id` | ❌ | ✅ |
| `FaturaNfe` | ✅ via `empresa` | ❌ | ✅ |
| `MetaMecanico` | ✅ via `mecanico` | ❌ | — |
| `UserConsent` | ✅ via `user` | ❌ | ✅ |
| `DataSubjectRequest` | ✅ `empresa_id` | ❌ | ✅ |
| `AuditLog` | ✅ `empresa_id` | ❌ | — (é a auditoria) |
| `RefreshToken` | ✅ via `user` | ❌ | — |
| `EmpresaConfig` | ✅ `empresa_id` | ❌ | — |
| `BaseAuditEntity` | — (superclasse) | — | `created_at/by`, `updated_at/by` |

### Feature Flags Atuais (8)

```
VEHICLE_MANAGEMENT      → Starter+
DOCUMENT_VAULT          → Starter+
MAINTENANCE_MODULE      → Starter+
GOALS_MODULE            → Professional+
FINANCIAL_MODULE        → Professional+
ANALYTICS_DASHBOARD     → Professional+
NOTIFICATIONS           → Professional+ (implementado parcialmente)
DETRAN_INTEGRATION      → Enterprise
```

### Migrations Flyway (V1–V8)

| Versão | Conteúdo |
|---|---|
| V1 | Schema inicial |
| V2 | Campos de manutenção + empresa_config |
| V3 | tracking_token |
| V4 | Multi-tenant (empresas) |
| V5 | Metas por mecânico |
| V6 | Billing (assinaturas, faturas_nfe) |
| V7 | Dados fiscais da empresa (NFS-e) |
| V8 | LGPD (user_consents, data_subject_requests, audit_logs) |

**Próxima migration disponível: V9**

### Restrições Arquiteturais Obrigatórias

Toda nova funcionalidade **deve** respeitar:

1. **Multi-Tenant** — `empresa_id` ou relação transitiva em todas as entidades tenant-scoped
2. **RBAC** — `@PreAuthorize` ou `roleGuard` em todo endpoint/rota protegida
3. **LGPD** — PII em novas entidades deve aparecer em: export `my-data`, `anonymizeUser()`, `DataRetentionService`
4. **Audit Logs** — `AuditLogRepository.save()` em toda operação sobre dados de titulares
5. **Flyway** — nenhuma alteração de schema sem migration versionada
6. **Feature Flags** — todo módulo novo deve ter uma `AppFeatures` entry correspondente
7. **Clean Architecture** — Controller → Service → Repository → Domain; sem `@Entity` no Controller
8. **DTO Pattern** — entidades nunca expostas diretamente; sempre records de request/response
9. **Problem Details RFC 7807** — exceções de negócio sempre via `GlobalExceptionHandler`
10. **PlanEnforcementService** — limites quantitativos sempre verificados no backend

---

## FASE 1 — Notificações (WhatsApp / E-mail / SMS / Push)

**Migration:** V9  
**Feature Flag:** `NOTIFICATIONS` (já existe, ativar no Professional+)  
**Plano:** Professional+

### Problema Arquitetural Central

O sistema já tem a flag `NOTIFICATIONS` mas sem implementação de backend. A arquitetura precisa ser extensível para múltiplos canais sem acoplar código de negócio a nenhum provider específico.

### Decisão: Provider Pattern + Spring Events

```
ManutencaoService.criar()
    └── ApplicationEventPublisher.publish(OsCriadaEvent)
            └── NotificationEventListener.on(OsCriadaEvent)
                    └── NotificationService.enviar(empresaId, tipo, destinatario, variáveis)
                            └── NotificationProvider.send(NotificationMessage)
                                    ├── WhatsAppNotificationProvider (Evolution API / Meta)
                                    ├── EmailNotificationProvider (JavaMailSender)
                                    ├── SmsNotificationProvider (futuro)
                                    └── PushNotificationProvider (futuro — FCM)
```

**Por que Spring Events e não chamada direta?** Desacopla o domínio de negócio do sistema de notificações. `ManutencaoService` não conhece `NotificationService`. Facilita teste, manutenção e adição de novos gatilhos.

### Entidades Novas

```java
// Entidade 1: Template configurável por empresa e evento
@Entity @Table(name = "notification_templates")
NotificationTemplate {
    UUID id
    UUID empresaId          // tenant-scoped
    NotificationEvent evento // OS_CRIADA, OS_EM_ANDAMENTO, OS_CONCLUIDA, DOCUMENTO_VENCENDO
    NotificationChannel canal // WHATSAPP, EMAIL, SMS, PUSH
    String titulo
    String corpo            // com variáveis: {{cliente_nome}}, {{veiculo_placa}}, {{os_link}}
    boolean ativo
    // herda BaseAuditEntity
}

// Entidade 2: Log imutável de cada envio
@Entity @Table(name = "notification_logs")
NotificationLog {
    UUID id
    UUID empresaId
    UUID manutencaoId       // nullable — pode ser disparo de doc vencendo
    UUID clienteId
    NotificationEvent evento
    NotificationChannel canal
    String destinatario     // telefone mascarado ou email mascarado (LGPD)
    NotificationStatus status // PENDENTE, ENVIADO, FALHOU, REJEITADO
    String errorMessage     // nullable
    Instant enviadoEm
    // herda BaseAuditEntity
}
```

### Novos Enums

```java
enum NotificationEvent {
    OS_CRIADA, OS_EM_ANDAMENTO, OS_AGUARDANDO_PECAS,
    OS_CONCLUIDA, DOCUMENTO_VENCENDO, ORCAMENTO_AGUARDANDO
}

enum NotificationChannel { WHATSAPP, EMAIL, SMS, PUSH }

enum NotificationStatus { PENDENTE, ENVIADO, FALHOU, REJEITADO }
```

### Interface Central

```java
public interface NotificationProvider {
    NotificationChannel getChannel();
    boolean supports(NotificationChannel channel);
    NotificationResult send(NotificationMessage message);
}

public record NotificationMessage(
    String destinatario,
    String titulo,
    String corpo,
    Map<String, String> variaveis
) {}
```

### Migration V9

```sql
CREATE TABLE notification_templates (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID NOT NULL REFERENCES empresas(id),
    evento       VARCHAR(50) NOT NULL,
    canal        VARCHAR(20) NOT NULL,
    titulo       VARCHAR(200),
    corpo        TEXT NOT NULL,
    ativo        BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(180),
    updated_by   VARCHAR(180),
    UNIQUE (empresa_id, evento, canal)
);

CREATE TABLE notification_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id      UUID NOT NULL REFERENCES empresas(id),
    manutencao_id   UUID REFERENCES manutencoes(id),
    cliente_id      UUID REFERENCES clientes(id),
    evento          VARCHAR(50) NOT NULL,
    canal           VARCHAR(20) NOT NULL,
    destinatario    VARCHAR(200),       -- mascarado para LGPD
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    error_message   TEXT,
    enviado_em      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_logs_empresa ON notification_logs(empresa_id);
CREATE INDEX idx_notif_logs_manutencao ON notification_logs(manutencao_id);
```

### APIs REST

```
GET  /api/v1/notifications/templates          → lista templates da empresa (GERENTE+)
PUT  /api/v1/notifications/templates/{id}     → editar template (GERENTE+)
POST /api/v1/notifications/templates/reset    → restaurar padrão (ADMIN)
GET  /api/v1/notifications/logs               → histórico paginado (GERENTE+)
POST /api/v1/notifications/test/{templateId}  → disparo de teste (ADMIN)
```

### Camada de Serviço

```
NotificationService          → orquestra: resolve template, interpola variáveis, dispara, loga
NotificationEventListener    → @EventListener para eventos de domínio
WhatsAppNotificationProvider → implementa NotificationProvider via Evolution API
EmailNotificationProvider    → implementa NotificationProvider via JavaMailSender
NotificationScheduler        → @Scheduled — verifica documentos vencendo nos próximos 7 dias
```

### Telas Angular

| Rota | Acesso | Descrição |
|---|---|---|
| `/admin/notificacoes/templates` | GERENTE, ADMIN | Editor de templates com preview e variáveis disponíveis |
| `/admin/notificacoes/logs` | GERENTE, ADMIN | Histórico de envios com filtros e status |

### Permissões RBAC

| Operação | Roles |
|---|---|
| Ver/editar templates | ROLE_GERENTE, ROLE_ADMIN |
| Ver logs | ROLE_GERENTE, ROLE_ADMIN |
| Teste manual | ROLE_ADMIN |
| Reset de templates | ROLE_ADMIN |

### Impactos Multi-Tenant

- `NotificationTemplate` e `NotificationLog` têm `empresa_id`
- Templates padrão criados pelo `DataInitializer` para cada empresa nova
- Cada empresa configura seus próprios templates e provedor (número de WhatsApp, e-mail remetente)
- `EmpresaConfig` ganha campos: `whatsapp_api_url`, `whatsapp_token` (criptografado)

### Impactos LGPD

- `NotificationLog.destinatario` armazena apenas os últimos 4 dígitos do telefone (ex: `***-**34`)
- Consentimento para receber notificações: novo campo `aceita_notificacoes` em `Cliente`
- `DataRetentionService` deve apagar `NotificationLog` após 90 dias
- `LgpdService.anonymizeUser()` deve apagar logs associados ao cliente

### Diagrama de Fluxo

```
Evento de domínio (OS concluída)
    ↓
NotificationEventListener.onOsConcluida()
    ↓
NotificationService.processar(empresaId, OS_CONCLUIDA, clienteId, variaveis)
    ↓ (busca template ativo para empresa+evento+canal)
NotificationTemplate encontrado?
    ├── SIM → interpola variáveis → NotificationProvider.send()
    │            ├── WhatsApp → Evolution API → HTTP 200
    │            └── Email    → JavaMailSender → SMTP
    └── NÃO → loga REJEITADO (sem template configurado)
    ↓
NotificationLog.status = ENVIADO | FALHOU
```

---

## FASE 2 — Estoque Inteligente

**Migration:** V10  
**Feature Flag:** `STOCK_MODULE` (nova, Professional+)  
**Plano:** Professional+

### Problema Arquitetural Central

Estoque precisa de rastreabilidade bidirecional: saber quais OS consumiram quais peças, e quais peças estão abaixo do mínimo. A integração com `Manutencao` deve ser opcional (OS pode existir sem consumo de estoque).

### Decisão: Movimentação Dupla Entrada/Saída

Cada alteração de estoque é uma `MovimentacaoEstoque` imutável. O estoque atual é calculado por soma de movimentações OU mantido como campo desnormalizado em `Produto` (opção mais performática adotada).

### Entidades Novas

```java
@Entity @Table(name = "categorias_produto")
CategoriaProduto {
    UUID id
    UUID empresaId
    String nome
    String descricao
    // herda BaseAuditEntity
}

@Entity @Table(name = "fornecedores")
Fornecedor {
    UUID id
    UUID empresaId
    String nome
    String cnpj             // nullable — fornecedor pode ser pessoa física
    String telefone
    String email
    String contato          // nome do responsável
    boolean ativo
    // herda BaseAuditEntity
}

@Entity @Table(name = "produtos")
Produto {
    UUID id
    UUID empresaId
    UUID categoriaId        // FK CategoriaProduto
    UUID fornecedorId       // FK Fornecedor, nullable
    String codigo           // código interno ou código de barras
    String nome
    String descricao
    String unidade          // UN, KG, L, M
    BigDecimal precoUnitario
    BigDecimal precoCusto
    int estoqueAtual        // desnormalizado, atualizado a cada movimentação
    int estoqueMinimo
    int estoqueMaximo       // nullable
    boolean ativo
    // herda BaseAuditEntity
}

@Entity @Table(name = "movimentacoes_estoque")
MovimentacaoEstoque {
    UUID id
    UUID empresaId
    UUID produtoId
    UUID manutencaoId       // nullable — vínculo com OS
    UUID userId             // quem fez a movimentação
    TipoMovimentacao tipo   // ENTRADA, SAIDA, AJUSTE, INVENTARIO
    int quantidade          // sempre positivo; tipo determina se subtrai
    BigDecimal valorUnitario
    String motivo           // COMPRA, USO_OS, AVARIA, AJUSTE_INVENTARIO
    String observacao
    Instant realizadaEm
    // herda BaseAuditEntity (sem updated_at — imutável)
}

// Junção OS ↔ Peças (muitos-para-muitos com quantidade)
@Entity @Table(name = "os_produtos")
OsProduto {
    UUID id
    UUID manutencaoId
    UUID produtoId
    int quantidade
    BigDecimal valorUnitario
    // herda BaseAuditEntity
}
```

### Migration V10

```sql
CREATE TABLE categorias_produto (...);
CREATE TABLE fornecedores (...);
CREATE TABLE produtos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID NOT NULL REFERENCES empresas(id),
    categoria_id      UUID REFERENCES categorias_produto(id),
    fornecedor_id     UUID REFERENCES fornecedores(id),
    codigo            VARCHAR(100),
    nome              VARCHAR(200) NOT NULL,
    unidade           VARCHAR(10) NOT NULL DEFAULT 'UN',
    preco_unitario    NUMERIC(12,2) NOT NULL DEFAULT 0,
    preco_custo       NUMERIC(12,2),
    estoque_atual     INTEGER NOT NULL DEFAULT 0,
    estoque_minimo    INTEGER NOT NULL DEFAULT 0,
    estoque_maximo    INTEGER,
    ativo             BOOLEAN NOT NULL DEFAULT true,
    ...
    UNIQUE (empresa_id, codigo)
);
CREATE TABLE movimentacoes_estoque (...);
CREATE TABLE os_produtos (...);
```

### APIs REST

```
GET  /api/v1/estoque/produtos                 → lista paginada com filtros (GERENTE+)
POST /api/v1/estoque/produtos                 → criar produto (GERENTE+)
PUT  /api/v1/estoque/produtos/{id}            → editar (GERENTE+)
GET  /api/v1/estoque/produtos/alertas         → produtos abaixo do mínimo (GERENTE+)
GET  /api/v1/estoque/movimentacoes            → histórico paginado (GERENTE+)
POST /api/v1/estoque/movimentacoes            → entrada manual (GERENTE+)
GET  /api/v1/estoque/fornecedores             → CRUD fornecedores (GERENTE+)
POST /api/v1/manutencoes/{id}/produtos        → adicionar peça à OS (MECANICO, GERENTE)
DELETE /api/v1/manutencoes/{id}/produtos/{pid}→ remover peça da OS
```

### Integração com ManutencaoService

```java
// Ao adicionar peça à OS:
OsProduto osProduto = osProdutoRepository.save(new OsProduto(os, produto, qtd, valorUnit));
MovimentacaoEstoque mov = MovimentacaoEstoque.saida(produto, qtd, "USO_OS", os);
movimentacaoRepository.save(mov);
produto.setEstoqueAtual(produto.getEstoqueAtual() - qtd);
produtoRepository.save(produto);

// Ao cancelar OS: reverter movimentações → criar ENTRADA de estorno
```

### PlanLimits — Novo Limite

```java
STARTER(    ..., 100,   ...),  // max 100 produtos
PROFESSIONAL(..., -1,   ...),  // ilimitado
ENTERPRISE( ..., -1,   ...)    // ilimitado
```

### Impactos LGPD

- `Fornecedor` tem `email` e `telefone` (dados de contato empresarial, não PII de titular)
- Sem impacto na anonimização de usuários
- `MovimentacaoEstoque` referencia `userId` — manter por prazo fiscal (5 anos), não apagar em 90 dias

---

## FASE 3 — Portal Completo do Cliente

**Migration:** V11  
**Feature Flag:** `CLIENTE_PORTAL` (nova, Professional+)  
**Plano:** Professional+

### Decisão Arquitetural: Token de Portal (sem novo sistema de login)

**Alternativa A:** Criar `ClienteUser` com senha própria e sistema de auth separado  
**Alternativa B:** Token de acesso por CPF/CNPJ + código enviado por WhatsApp (OTP)  
**Escolha: B** — menor complexidade, sem gerenciamento de senhas de clientes, alinhado com LGPD (menos dados armazenados)

### Fluxo de Acesso

```
Cliente acessa https://managerpitstop.com.br/portal
    → informa CPF/CNPJ
    → sistema envia código OTP via WhatsApp (fase 1 já implementada)
    → cliente insere código → recebe portal_access_token (JWT com claims limitadas)
    → acessa seus dados por 48h
```

### Entidades Novas

```java
@Entity @Table(name = "cliente_portal_otps")
ClientePortalOtp {
    UUID id
    UUID clienteId
    String otpHash          // SHA-256 do código de 6 dígitos
    Instant expiresAt       // 10 minutos
    boolean usado
    String ipAddress
    Instant createdAt
}
```

Campos novos em entidades existentes:

```sql
-- Em manutencoes:
ALTER TABLE manutencoes ADD COLUMN garantia_expira_em TIMESTAMPTZ;
ALTER TABLE manutencoes ADD COLUMN nota_para_cliente TEXT; -- visível no portal
```

### APIs REST (prefixo `/api/v1/portal` — sem authGuard JWT)

```
POST /api/v1/portal/auth/otp          → solicitar OTP (rate limited: 3/hora por CPF)
POST /api/v1/portal/auth/verify       → verificar OTP → retorna portal_access_token
GET  /api/v1/portal/veiculos          → veículos do cliente autenticado
GET  /api/v1/portal/os                → histórico de OS
GET  /api/v1/portal/os/{tracking}     → detalhes com tracking_token (já existe)
GET  /api/v1/portal/documentos        → CRLV e laudos (URL pré-assinada MinIO)
GET  /api/v1/portal/garantias         → OS com garantia_expira_em futura
```

### SecurityConfig — Nova Chain de Filtros

```java
// Cadeia separada para rotas /api/v1/portal/** com PortalTokenFilter
// PortalTokenFilter usa JWT com audience="portal" e subject=clienteId
// Sem acesso ao TenantContext do sistema principal
```

### Impactos LGPD

- `ClientePortalOtp.ipAddress` é dado pessoal → apagar em 30 dias
- Portal expõe dados do próprio titular → Art. 18, I LGPD (acesso garantido)
- Audit log de cada acesso ao portal com clienteId, IP, timestamp
- OTP não armazenado em plain-text — apenas hash SHA-256

---

## FASE 4 — Assinatura Digital

**Migration:** V12  
**Feature Flag:** `DIGITAL_SIGNATURE` (nova, Professional+)  
**Plano:** Professional+

### Decisão Arquitetural: Assinatura Probatória (não ICP-Brasil)

**Escopo:** Evidência digital de aceite, não certificado ICP-Brasil (que requer integração com AC).  
**Valor jurídico:** Marco Civil da Internet (Lei 12.965/2014) — IP + timestamp + hash são evidências válidas de aceite eletrônico para fins probatórios.

### Fluxo de Assinatura

```
1. Gerente gera link de assinatura (POST /manutencoes/{id}/assinar/link)
2. Sistema gera PDF do orçamento/autorização → calcula hash SHA-256
3. Cliente recebe link único por WhatsApp/email
4. Cliente acessa página pública /assinar/{token}
5. Visualiza documento renderizado
6. Clica "Li e aceito" → POST /assinar/{token}/confirmar
7. Sistema registra: IP, userAgent, timestamp, hash_confirmado = hash_gerado
8. Cria AssinaturaDigital imutável
9. PDF com registro de assinatura gerado e armazenado
```

### Entidades Novas

```java
@Entity @Table(name = "assinaturas_digitais")
AssinaturaDigital {
    UUID id
    UUID empresaId
    UUID manutencaoId
    TipoAssinatura tipo      // ORCAMENTO, AUTORIZACAO_SERVICO, ENTREGA_VEICULO
    String nomeSignatario
    String documentoHash     // SHA-256 do PDF no momento da assinatura
    String ipAddress
    String userAgent
    Instant assinadoEm
    UUID tokenAcesso         // token de link único (hash após uso)
    boolean tokenUsado
    Instant tokenExpiresAt
    // NÃO herda BaseAuditEntity — imutável por design
}
```

### Restrição LGPD: Retenção Especial

```java
// Em DataRetentionService:
// AssinaturaDigital NÃO deve ser apagada nos 90 dias padrão
// Prazo legal para documentos contratuais: 5 anos (CC Art. 206 §5 I)
// Campo retention_category = "LEGAL_DOCUMENT" → excluído da limpeza de 90 dias
```

### APIs REST

```
POST /api/v1/manutencoes/{id}/assinatura/link        → gerar link (GERENTE+)
GET  /api/v1/manutencoes/{id}/assinatura/status       → verificar assinatura
GET  /api/v1/assinatura/{token}                       → página pública do documento
POST /api/v1/assinatura/{token}/confirmar             → registrar assinatura
GET  /api/v1/assinatura/{id}/verify                   → verificar hash (público)
```

---

## FASE 5 — API para Mobile (Flutter)

**Migration:** V13  
**Feature Flag:** `MOBILE_API` (nova, Professional+)  
**Plano:** Professional+

### Decisão: Endpoints Mobile Dedicados vs. Reutilização

**Escolha:** Endpoints mobile separados em `/api/mobile/v1/` com payloads compactos, mas usando os mesmos serviços do web. Evita duplicação de lógica de negócio.

### Entidades Novas

```java
@Entity @Table(name = "device_tokens")
DeviceToken {
    UUID id
    UUID userId
    String fcmToken          // Firebase Cloud Messaging token
    MobilePlatform platform  // IOS, ANDROID
    String appVersion
    Instant lastSeenAt
    boolean ativo
    Instant createdAt
}
```

Campos novos em `Manutencao`:

```sql
-- Fotos de OS (array de storage keys MinIO)
ALTER TABLE manutencoes ADD COLUMN foto_keys TEXT[]; -- array de StorageKeys
```

### APIs REST Mobile (`/api/mobile/v1/`)

```
-- Mecânico
GET  /mecanico/os                      → OS do mecânico com paginação compacta
GET  /mecanico/os/{id}                 → detalhes + checklist
PATCH /mecanico/os/{id}/status         → atualizar status
POST /mecanico/os/{id}/fotos           → upload de fotos (multipart, max 5MB cada)
DELETE /mecanico/os/{id}/fotos/{key}   → remover foto

-- Gerente  
GET  /gerente/dashboard                → KPIs compactos
GET  /gerente/os                       → todas as OS da empresa

-- Compartilhado
POST /auth/device-token                → registrar FCM token
DELETE /auth/device-token              → revogar token do dispositivo
```

### Impactos LGPD

- Fotos de veículos podem capturar pessoas inadvertidamente → consentimento no ToS
- `DeviceToken.fcmToken` é dado de dispositivo (não PII diretamente, mas vinculado ao usuário)
- `LgpdService.anonymizeUser()` deve revogar todos os `DeviceToken` do usuário

---

## FASE 6 — Multi-Filial

**Migration:** V14  
**Feature Flag:** `MULTI_FILIAL` (nova, Enterprise only)  
**Plano:** Enterprise

### Decisão Arquitetural: Sub-Tenant sem Quebrar o Multi-Tenant Atual

**Princípio:** `empresa_id` continua sendo o tenant principal e isolador de dados. `filial_id` é um sub-escopo opcional. Empresas sem filiais continuam funcionando sem nenhuma alteração.

### Modelo de Dados

```
Empresa (tenant raiz)
│  id, nome, cnpj, subscription_plan = ENTERPRISE
│
├── Filial (sub-scope)
│   id, empresa_id, nome, cnpj, endereço
│
├── User (empresa_id + filial_id nullable)
│   Gerente de filial → filial_id preenchido
│   Admin da empresa  → filial_id NULL (vê tudo)
│
├── Cliente (empresa_id + filial_id nullable)
└── Manutencao (empresa_id via mecanico + filial_id nullable)
```

### Migration V14

```sql
CREATE TABLE filiais (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL REFERENCES empresas(id),
    nome        VARCHAR(150) NOT NULL,
    cnpj        VARCHAR(18),
    logradouro  VARCHAR(200),
    cidade      VARCHAR(100),
    uf          VARCHAR(2),
    telefone    VARCHAR(20),
    ativo       BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Adicionar filial_id opcional (retrocompatível — NULL = sem filial)
ALTER TABLE users       ADD COLUMN filial_id UUID REFERENCES filiais(id);
ALTER TABLE clientes    ADD COLUMN filial_id UUID REFERENCES filiais(id);
ALTER TABLE manutencoes ADD COLUMN filial_id UUID REFERENCES filiais(id);
```

### TenantContext — Extensão

```java
// TenantDetails ganha filialId opcional
public record TenantDetails(String remoteAddress, UUID empresaId, UUID userId, UUID filialId) {}

// JWT claim "filialId" opcional (gerentes de filial têm, admins não)
```

### Regras de Visibilidade

| Role | Filial no JWT | Vê |
|---|---|---|
| ROLE_ADMIN | null | Todos os dados de todas as filiais |
| ROLE_GERENTE (empresa) | null | Todas as filiais |
| ROLE_GERENTE (filial) | `filialId` preenchido | Apenas os dados da sua filial |
| ROLE_MECANICO | `filialId` da filial | Apenas OS da sua filial |

### APIs REST

```
GET  /api/v1/filiais              → lista filiais da empresa (ADMIN, GERENTE)
POST /api/v1/filiais              → criar filial (ADMIN)
PUT  /api/v1/filiais/{id}         → editar filial (ADMIN)
GET  /api/v1/filiais/{id}/stats   → stats consolidadas da filial (ADMIN, GERENTE)
GET  /api/v1/dashboard/consolidado → KPIs de todas as filiais (ADMIN)
```

### Impactos LGPD

- Anonimização percorre todas as filiais da empresa
- `filial_id` incluído no export de portabilidade (`my-data`)
- DSARs da empresa cobrem dados de todas as filiais

---

## FASE 7 — API Pública

**Migration:** V15  
**Feature Flag:** `PUBLIC_API` (nova, Enterprise only)  
**Plano:** Enterprise

### Decisão: API Keys com Escopos (OAuth2-like sem OAuth2)

Não implementar OAuth2 completo (complexidade desnecessária para MVP). Usar API Keys com escopos explícitos e rate limiting por key.

### Entidades Novas

```java
@Entity @Table(name = "api_keys")
ApiKey {
    UUID id
    UUID empresaId
    String nome              // "Integração ERP Totvs"
    String keyHash           // SHA-256 da key bruta (nunca armazenar plain)
    String keyPrefix         // primeiros 8 chars para identificação visual: "ps_live_abcd1234..."
    Set<String> escopos      // ["os:read", "os:write", "clientes:read", "veiculos:read"]
    boolean ativo
    Instant expiresAt        // nullable — chaves podem não expirar
    Instant lastUsedAt
    String lastUsedIp
    long requestCount
    // herda BaseAuditEntity
}

@Entity @Table(name = "api_key_usage_logs")
ApiKeyUsageLog {
    UUID id
    UUID apiKeyId
    UUID empresaId
    String endpoint
    String method
    int statusCode
    long durationMs
    Instant timestamp
    String ipAddress
}
```

### Escopos Disponíveis

```
os:read          → GET em /api/public/v1/os
os:write         → POST/PUT em /api/public/v1/os
clientes:read    → GET em /api/public/v1/clientes
veiculos:read    → GET em /api/public/v1/veiculos
estoque:read     → GET em /api/public/v1/estoque/produtos
webhooks:write   → registrar webhooks de saída
```

### Autenticação da API Pública

```java
// Header: Authorization: ApiKey ps_live_abcd1234...xyz
// ApiKeyAuthenticationFilter:
//   1. Extrai key do header
//   2. Calcula hash SHA-256
//   3. Busca no banco pelo hash
//   4. Verifica ativo, não expirado, escopo
//   5. Popula TenantContext com empresaId da key
//   6. Rate limit: 1000 req/hora por key (Bucket4j)
```

### Documentação OpenAPI

```java
// Adicionar springdoc-openapi
// @Tag, @Operation, @ApiResponse em todos os endpoints públicos
// Disponibilizar em /api/public/docs
```

---

## FASE 8 — Assistente de Inteligência Artificial

**Migration:** V16  
**Feature Flag:** `AI_ASSISTANT` (nova, Enterprise no início, expandir depois)  
**Plano:** Enterprise (MVP) → Professional+ (futuro)

### Decisão Arquitetural: AiProvider Interface com Zero Lock-in

**Princípio:** O código de negócio nunca chama OpenAI/Claude/Gemini diretamente. Toda IA passa pela interface `AiProvider`. Trocar de provider é alterar 1 linha de configuração.

### Interface Central

```java
public interface AiProvider {
    String getProviderName();
    boolean isAvailable();
    
    // Features específicas
    DiagnosisResult suggestDiagnosis(DiagnosisRequest request);
    OrcamentoResult draftOrcamento(OrcamentoRequest request);
    String summarizeOs(OsSummaryRequest request);
    String assistantMessage(AssistantRequest request);
}

// Request sempre inclui contexto sem PII:
public record DiagnosisRequest(
    String descricaoProblema,
    String marcaModelo,
    int anoFabricacao,
    int kmAtual,
    List<String> servicosAnteriores  // sem nome do cliente
) {}
```

### PII Masking antes de enviar à IA

```java
// Regra: dados enviados à IA NUNCA contêm PII do titular
// ManutencaoService.prepareForAi():
//   - Remove nome do cliente → "Cliente"
//   - Remove telefone/email
//   - Mantém: descrição do problema, marca/modelo, km, histórico de serviços
```

### Entidades Novas

```java
@Entity @Table(name = "empresa_ai_config")
EmpresaAiConfig {
    UUID id
    UUID empresaId
    AiProviderType provider   // OPENAI, CLAUDE, GEMINI, OLLAMA, SYSTEM (usa config global)
    String apiKeyEncrypted    // AesEncryptionService — chave do cliente
    String modelId            // "gpt-4o", "claude-3-5-sonnet", etc.
    boolean enabled
    int maxTokensPerRequest   // default: 2000
    // herda BaseAuditEntity
}

@Entity @Table(name = "ai_interactions")
AiInteraction {
    UUID id
    UUID empresaId
    UUID userId
    AiFeature feature         // DIAGNOSIS, ORCAMENTO, SUMMARY, ASSISTANT
    AiProviderType provider
    String modelId
    int promptTokens
    int completionTokens
    long durationMs
    boolean success
    String errorMessage       // nullable
    Instant createdAt
    // SEM conteúdo do prompt — privacidade e custo de storage
}
```

### APIs REST

```
GET  /api/v1/ai/config                   → configuração de IA da empresa (ADMIN)
PUT  /api/v1/ai/config                   → salvar config (ADMIN)
POST /api/v1/ai/manutencoes/{id}/diagnose → sugestão de diagnóstico
POST /api/v1/ai/manutencoes/{id}/orcamento → rascunho de orçamento
POST /api/v1/ai/manutencoes/{id}/summarize → resumo em linguagem natural
GET  /api/v1/ai/usage                    → consumo de tokens por período (ADMIN, GERENTE)
```

### Telas Angular

| Rota | Acesso | Descrição |
|---|---|---|
| `/admin/ia` | ADMIN | Configuração do provider, API key, modelo |
| `/admin/ia/uso` | ADMIN, GERENTE | Consumo de tokens e custo estimado |
| Botão inline na OS | MECANICO, GERENTE | "Sugerir diagnóstico", "Rascunhar orçamento" |

---

## Sequência Recomendada de Implementação

```
FASE 1 (Notificações)   → Fundação para Fases 3 e 4 (OTP e links de assinatura dependem disso)
FASE 4 (Assinatura)     → Alto valor comercial, baixa dependência
FASE 2 (Estoque)        → Módulo independente, alto valor para oficinas
FASE 3 (Portal Cliente) → Depende de Fase 1 (OTP por WhatsApp)
FASE 5 (Mobile API)     → Depende de Fase 1 (push notifications)
FASE 7 (API Pública)    → Independente, mas melhor após estabilização
FASE 6 (Multi-Filial)   → Maior impacto arquitetural — fazer no final do core
FASE 8 (IA)             → Diferencial competitivo — fazer após base consolidada
```

## Visão Geral das Novas Migrations

| Version | Fase | Conteúdo |
|---|---|---|
| V9  | Fase 1 | `notification_templates`, `notification_logs` |
| V10 | Fase 2 | `categorias_produto`, `fornecedores`, `produtos`, `movimentacoes_estoque`, `os_produtos` |
| V11 | Fase 3 | `cliente_portal_otps`, campos em `manutencoes` |
| V12 | Fase 4 | `assinaturas_digitais` |
| V13 | Fase 5 | `device_tokens`, `foto_keys` em `manutencoes` |
| V14 | Fase 6 | `filiais`, `filial_id` em `users/clientes/manutencoes` |
| V15 | Fase 7 | `api_keys`, `api_key_usage_logs` |
| V16 | Fase 8 | `empresa_ai_config`, `ai_interactions` |

## Novas Feature Flags

| Flag | Fase | Plano |
|---|---|---|
| `NOTIFICATIONS` | 1 | Professional+ (já existe) |
| `STOCK_MODULE` | 2 | Professional+ |
| `CLIENTE_PORTAL` | 3 | Professional+ |
| `DIGITAL_SIGNATURE` | 4 | Professional+ |
| `MOBILE_API` | 5 | Professional+ |
| `MULTI_FILIAL` | 6 | Enterprise |
| `PUBLIC_API` | 7 | Enterprise |
| `AI_ASSISTANT` | 8 | Enterprise |

## Impacto no PlanLimits

```java
// Additions to PlanLimits.java:
STARTER(
    50, 2, 5GB,
    100,          // max produtos em estoque
    Set.of(VEHICLE_MANAGEMENT, DOCUMENT_VAULT, MAINTENANCE_MODULE)
),
PROFESSIONAL(
    -1, -1, 50GB,
    -1,           // produtos ilimitados
    Set.of(...tudo do STARTER + NOTIFICATIONS, STOCK_MODULE,
              CLIENTE_PORTAL, DIGITAL_SIGNATURE, MOBILE_API,
              GOALS_MODULE, FINANCIAL_MODULE, ANALYTICS_DASHBOARD)
),
ENTERPRISE(
    -1, -1, -1L,
    -1,
    Set.of(AppFeatures.values())   // tudo
)
```

---

*Este documento deve ser consultado antes de iniciar cada fase. Cada fase só deve ser implementada após revisão e aprovação deste plano técnico.*

*© 2025 RiseCode Studio — Confidencial*
