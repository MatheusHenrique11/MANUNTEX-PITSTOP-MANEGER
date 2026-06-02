<h1 align="center">
  <br>
  Pitstop Manager
  <br>
</h1>

<p align="center">
  Plataforma SaaS multi-tenant para gestão completa de oficinas automotivas —<br>
  ordens de serviço, frotas, documentos, faturamento, metas e conformidade LGPD.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring Boot-3.3.2-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Angular-17-DD0031?style=flat-square&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/LGPD-Compliant-009c3b?style=flat-square" />
</p>

<p align="center">
  <strong>Produção:</strong>
  <a href="https://managerpitstop.com.br">managerpitstop.com.br</a>
  ·
  <a href="https://api.managerpitstop.com.br/actuator/health">api.managerpitstop.com.br</a>
</p>

---

> **Software Proprietário — © 2025 RiseCode Studio. Todos os direitos reservados.**
> Consulte a seção [Licença e Direitos Autorais](#licença-e-direitos-autorais) para os termos completos.

---

## Visão Geral

O **Manager PitStop** é um SaaS B2B desenvolvido pela **RiseCode Studio** para gestão de oficinas mecânicas. Cada empresa opera em seu próprio contexto completamente isolado (multi-tenant), com controle de acesso por perfil (RBAC), criptografia de ponta a ponta nos documentos, enforcement automático de limites por plano de assinatura, sistema de pagamentos com emissão automática de NFS-e e conformidade integral com a **Lei Geral de Proteção de Dados (LGPD — Lei 13.709/2018)**.

---

## Módulos e Funcionalidades

### Gestão Operacional

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Ordens de Serviço** | Abertura, atribuição a mecânico, ciclo de status (Aberta → Em Andamento → Concluída/Cancelada), relatório em PDF | `MAINTENANCE_MODULE` |
| **Rastreio Público** | Link com token único para o cliente acompanhar a OS sem login | — |
| **Gestão de Veículos** | Cadastro com placa, chassi (mascarado), RENAVAM (mascarado), marca/modelo e histórico de serviços | `VEHICLE_MANAGEMENT` |
| **Clientes** | CPF/CNPJ, telefone, e-mail; CPF mascarado por perfil; vinculação a múltiplos veículos | — |
| **Cofre de Documentos** | Upload, visualização e controle de validade (CRLV, laudos) com pipeline AES-256-GCM + S3 | `DOCUMENT_VAULT` |

### Gestão de Pessoas e Metas

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Metas por Mecânico** | Metas mensais de faturamento por mecânico, acompanhamento em tempo real e relatório PDF para RH | `GOALS_MODULE` |
| **Gestão de Usuários** | Criação, ativação/desativação e perfil (GERENTE, MECÂNICO, RECEPCIONISTA) com isolamento por empresa | — |

### Financeiro e Faturamento

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Financeiro** | Visão consolidada das OS concluídas por período | `FINANCIAL_MODULE` |
| **Assinaturas (Billing)** | Planos Starter / Professional / Enterprise via Stripe; trial; webhook HMAC-SHA256 | — |
| **NFS-e Automática** | Emissão via Focus NFe a cada `invoice.paid`; idempotência por `gatewayInvoiceId` | — |
| **Dashboard de Faturamento** | Histórico de faturas com PDF/XML das NFS-e | — |

### Analítico e Relatórios

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Dashboard** | KPIs consolidados: OS abertas, em andamento, concluídas e valor total | — |
| **Relatórios** | Análises por período, mecânico e tipo de serviço | `ANALYTICS_DASHBOARD` |

### Configuração e Administração

| Módulo | Descrição | Perfil |
|---|---|---|
| **Controle de Módulos** | Toggle de feature flags por empresa em tempo real via painel Angular | `ROLE_ADMIN` |
| **Gestão de Empresas** | Cadastro de novos tenants com dados fiscais completos | `ROLE_ADMIN` |
| **Auto-cadastro (Signup)** | Formulário público com validação matemática de CNPJ e preenchimento via BrasilAPI | — |
| **Configuração da Empresa** | Logo, dados fiscais para NFS-e | `ROLE_GERENTE` |

### Privacidade e LGPD

| Módulo | Descrição |
|---|---|
| **Consentimento** | Coleta por versão de política (PP + ToU) com IP e user-agent — Art. 8 LGPD |
| **Portal do Titular** | Acesso, portabilidade, correção, anonimização, oposição — Art. 18 LGPD |
| **Exportação de Dados** | JSON estruturado completo (portabilidade) |
| **Anonimização** | Substituição de PII + revogação de sessões + `deleted_at` |
| **Retenção Automática** | Job diário às 03h: hard-delete após 90 dias + limpeza de tokens expirados |
| **Audit Log** | Registro imutável de todas as operações sobre dados pessoais |

---

## Planos e Enforcement de Limites

O enforcement é aplicado no **backend** antes de cada operação. Tentar ultrapassar o limite retorna HTTP **402 Payment Required**.

| Recurso | STARTER | PROFESSIONAL | ENTERPRISE |
|---|---|---|---|
| **Preço** | R$ 89/mês | R$ 179/mês | R$ 349/mês |
| **OS por mês** | 50 | Ilimitado | Ilimitado |
| **Mecânicos ativos** | 2 | Ilimitado | Ilimitado |
| **Armazenamento** | 5 GB | 50 GB | Ilimitado |
| Módulo Manutenções | ✅ | ✅ | ✅ |
| Gestão de Veículos | ✅ | ✅ | ✅ |
| Cofre de Documentos | ✅ | ✅ | ✅ |
| Metas / Financeiro / Relatórios | ❌ | ✅ | ✅ |
| Notificações | ❌ | ✅ | ✅ |
| Integração DETRAN | ❌ | ❌ | ✅ |
| API Pública | ❌ | ❌ | ✅ |

**Fluxo de ativação:** ao confirmar o pagamento via webhook Stripe, o `PlanEnforcementService.activateFeaturesForPlan()` ativa automaticamente as feature flags correspondentes ao plano adquirido e desativa as demais.

---

## Perfis de Acesso (RBAC)

| Perfil | Permissões |
|---|---|
| `ROLE_ADMIN` | Acesso total; gerencia todas as empresas, usuários, feature flags e DSARs globais |
| `ROLE_GERENTE` | Cria e gerencia OS, metas, usuários da própria empresa; acessa faturamento e relatórios |
| `ROLE_MECANICO` | Visualiza apenas as próprias OS e metas do mês; sem acesso a dados financeiros |
| `ROLE_RECEPCIONISTA` | Abre e consulta OS; sem acesso a financeiro, metas ou relatórios |

> Chassi, RENAVAM e CPF completo são mascarados para `ROLE_MECANICO` e `ROLE_RECEPCIONISTA`.

---

## Stack Tecnológica

### Backend — Java 21 / Spring Boot 3.3.2

| Tecnologia | Uso |
|---|---|
| **Spring Security** | JWT em cookies HTTP-Only + SameSite; refresh token rotation; RBAC via `@PreAuthorize` |
| **Spring Data JPA** | PostgreSQL 15 + Hibernate 6; `@SQLRestriction` para soft-delete transparente |
| **Flyway** | Migrations versionadas (V1–V8); baseline automático |
| **Togglz** | Feature flags com toggle via API REST; estado persistido no banco |
| **Bucket4j** | Rate limiting por IP (60 req/min) com header `X-RateLimit-Remaining` |
| **OpenPDF** | Relatórios mensais de metas em PDF |
| **MinIO / S3** | Armazenamento de documentos; URLs pré-assinadas com expiração de 5 min |
| **RestClient** (Spring 6.1) | Integrações HTTP com BrasilAPI, Focus NFe e Stripe |
| **Hibernate Validator** | `@CNPJ`, `@CPF`, validadores customizados de placa, chassi e RENAVAM |

### Frontend — Angular 17

| Tecnologia | Uso |
|---|---|
| **Standalone Components** | Sem `NgModule`; lazy loading por rota via `loadComponent` |
| **Signals** | Estado reativo (`signal`, `computed`) para auth, flags e consentimento |
| **Reactive Forms** | Validadores customizados (CNPJ matemático, confirmação de senha) |
| **HttpClient + Interceptors** | `credentials`, `auth-refresh` (renovação silenciosa) e `error` |
| **Guards encadeados** | `authGuard` → `consentGuard` → `subscriptionGuard` → `featureFlagGuard` → `roleGuard` |
| **Tailwind CSS** | Design system customizado: paleta `petroleum`, `safety`, `surface` |

### Infraestrutura e Deploy

| Componente | Ambiente | Descrição |
|---|---|---|
| **Vercel** | Produção (frontend) | Deploy automático; `set-env.js` injeta `VITE_API_URL` em build time |
| **VPS (Docker + Traefik)** | Produção (backend) | Spring Boot em container; Traefik como reverse proxy com TLS automático |
| **PostgreSQL 15** | VPS | Banco de dados principal |
| **MinIO** | VPS | Object storage S3-compatível para documentos cifrados |
| **Docker Compose** | Desenvolvimento | PostgreSQL + MinIO provisionados automaticamente |
| **BrasilAPI** | Integração | Consulta de CNPJ pública, sem chave de API |
| **Stripe** | Integração | Pagamentos; webhook validado por HMAC-SHA256 |
| **Focus NFe** | Integração | Emissão de NFS-e em homologação e produção |

---

## Segurança

### Autenticação e Sessão
- JWT em cookies `HttpOnly + Secure + SameSite=None` — nunca no `localStorage`
- Claims JWT: `subject` (email), `roles`, `empresaId`, `userId`
- Refresh tokens persistidos apenas como hash SHA-256
- Rotação automática de refresh token a cada uso
- Detecção de roubo: segundo uso do mesmo refresh token revoga toda a família

### Dados em Repouso
- Documentos cifrados com **AES-256-GCM** + IV aleatório de 12 bytes antes do upload
- `storageKey` nunca exposta ao frontend — download via URLs pré-assinadas com expiração
- Senhas com hash **BCrypt** custo 12

### Proteção de API
- Rate limiting por IP em todas as rotas
- Validação de arquivos por **Magic Numbers** (não por extensão)
- HMAC-SHA256 na validação de webhooks Stripe
- Headers: CSP, `frame-ancestors 'none'`, Referrer-Policy, Permissions-Policy

### Isolamento Multi-Tenant
- `empresaId` e `userId` propagados pelo JWT e verificados em todas as queries
- Sem possibilidade de acesso cross-tenant por design

### Mascaramento de Dados Sensíveis
- CPF/CNPJ de clientes: completo apenas para `ROLE_ADMIN` e `ROLE_GERENTE`
- Chassi e RENAVAM: apenas últimos 3–4 dígitos para mecânicos e recepcionistas

---

## Conformidade LGPD (Lei 13.709/2018)

| Artigo | Requisito | Implementação |
|---|---|---|
| Art. 8 | Consentimento livre e informado | `UserConsent` com versão, IP e user-agent; tela `/consent` obrigatória |
| Art. 9 | Transparência | Política de Privacidade e Termos de Uso públicos e versionados |
| Art. 15 | Encerramento do tratamento | Hard-delete automático após 90 dias (`DataRetentionService`) |
| Art. 18, I–II | Acesso e confirmação | `GET /api/v1/lgpd/my-data` exporta JSON completo |
| Art. 18, III | Correção | DSAR tipo `CORRECTION` com prazo de 15 dias |
| Art. 18, IV–VI | Anonimização e eliminação | `LgpdService.anonymizeUser()` substitui PII + revoga sessões |
| Art. 18, V | Portabilidade | Export JSON estruturado com download direto |
| Art. 18, IX | Oposição | DSAR tipo `OBJECTION` |
| Art. 46 | Medidas de segurança | `AuditLog` imutável em todas as operações sobre PII |

**DPO:** privacidade@manutex.com.br · **ANPD:** [gov.br/anpd](https://www.gov.br/anpd)

---

## Integrações Externas

### BrasilAPI — Consulta de CNPJ
```
GET /api/v1/tenants/lookup-cnpj/{cnpj}
```
Preenche razão social, endereço e dados fiscais automaticamente. Sem chave de API.

### Stripe — Pagamentos
- Sessão de checkout por plano (`criarCheckout`)
- `invoice.paid` → ativa assinatura + ativa feature flags do plano + emite NFS-e
- `customer.subscription.deleted` → suspende acesso
- `invoice.payment_failed` → altera status para `PAST_DUE`

### Focus NFe — Nota Fiscal de Serviço
- Emissão automática após `invoice.paid` com dados fiscais completos do Tomador
- Idempotência via `gatewayInvoiceId` — sem NFS-e duplicada em retentativas
- Mock local quando `FOCUS_NFE_TOKEN` não está configurado

---

## Como Executar

### Pré-requisitos
- Docker e Docker Compose
- Java 21
- Node.js 20+ e npm

### 1. Infraestrutura

```bash
docker compose up -d
```

| Serviço | URL |
|---|---|
| PostgreSQL | `localhost:5432` |
| MinIO API | `localhost:9000` |
| MinIO Console | `localhost:9001` |

### 2. Backend

```bash
cd backend
cp .env.example .env   # configure as variáveis
./mvnw spring-boot:run
```

API disponível em `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

Aplicação disponível em `http://localhost:4200`.

---

## Variáveis de Ambiente

### Obrigatórias em produção

| Variável | Descrição |
|---|---|
| `DATABASE_URL` | JDBC URL do PostgreSQL |
| `JWT_SECRET` | Segredo HS512 — mínimo 64 caracteres |
| `ENCRYPTION_MASTER_KEY` | Chave AES-256 para documentos (Base64) |
| `CORS_ALLOWED_ORIGINS` | Origens CORS (ex: `https://managerpitstop.com.br`) |

### Variável do frontend (Vercel)

| Variável | Valor em produção |
|---|---|
| `VITE_API_URL` | `https://api.managerpitstop.com.br/api` |

> O script `set-env.js` lê `VITE_API_URL` em build time e gera `environment.prod.ts` com `apiUrl = VITE_API_URL + '/v1'`.

### Integrações (opcionais em desenvolvimento)

| Variável | Descrição |
|---|---|
| `STRIPE_SECRET_KEY` | Chave Stripe (mock ativo quando vazio) |
| `STRIPE_WEBHOOK_SECRET` | Segredo de validação HMAC-SHA256 |
| `FOCUS_NFE_TOKEN` | Token Focus NFe (mock ativo quando vazio) |
| `STORAGE_ENDPOINT` | Endpoint S3-compatível (MinIO em dev) |
| `STORAGE_BUCKET` | Nome do bucket (padrão: `pitstop-docs`) |

---

## Testes

### Backend — 218 testes

```bash
cd backend
./mvnw test
```

| Camada | Ferramenta | Exemplos de cobertura |
|---|---|---|
| Serviços | `MockitoExtension` | Regras de negócio, enforcement de plano, LGPD |
| Controllers | `@WebMvcTest` | Autorização por perfil, respostas HTTP |
| Filtros | Mockito | Rate limiting, validação JWT |

### Frontend

```bash
cd frontend
npm test   # requer ChromeHeadless
```

---

## Migrações do Banco (Flyway)

| Versão | Descrição |
|---|---|
| **V1** | Schema inicial: `users`, `clientes`, `veiculos`, `documentos`, `manutencoes`, `refresh_tokens`, `feature_toggles` |
| **V2** | Campos de manutenção estendidos + tabela `empresa_config` |
| **V3** | Coluna `tracking_token` em `manutencoes` (rastreio público) |
| **V4** | Multi-tenant: tabela `empresas` + `empresa_id` em `users` e `clientes` |
| **V5** | Metas por mecânico: tabela `metas_mecanico` |
| **V6** | Billing: tabelas `assinaturas` e `faturas_nfe`; campos de assinatura em `empresas` |
| **V7** | Dados fiscais em `empresas` (razão social, endereço, e-mail/telefone fiscal para NFS-e) |
| **V8** | LGPD: `deleted_at` em `users`/`clientes`; tabelas `user_consents`, `data_subject_requests`, `audit_logs` |

---

## Estrutura do Projeto

```
Manutex-PitStop-Manager/
├── backend/
│   ├── src/main/java/com/manutex/pitstop/
│   │   ├── config/           # SecurityConfig, AppFeatures (Togglz), JpaAuditing
│   │   ├── domain/
│   │   │   ├── entity/       # User, Empresa, Cliente, Veiculo, Manutencao,
│   │   │   │                 #   Assinatura, FaturaNfe, UserConsent, AuditLog…
│   │   │   ├── enums/        # UserRole, SubscriptionPlan, PlanLimits,
│   │   │   │                 #   StatusManutencao, ConsentType, DsarType…
│   │   │   ├── repository/   # Spring Data JPA repositories
│   │   │   └── validation/   # @Placa, @Chassi, @Renavam
│   │   ├── security/         # JwtService, JwtAuthenticationFilter,
│   │   │                     #   TenantContext, TenantDetails
│   │   ├── service/          # AuthService, BillingService, LgpdService,
│   │   │                     #   PlanEnforcementService, ManutencaoService,
│   │   │                     #   DocumentoService, FocusNfeService,
│   │   │                     #   DataRetentionService, TokenCleanupService…
│   │   └── web/
│   │       ├── controller/   # Auth, Billing, Webhook, LGPD, Manutencao,
│   │       │                 #   Veiculo, Cliente, Documento, Meta,
│   │       │                 #   FeatureFlag, Tenant, EmpresaConfig, Rastreio
│   │       ├── dto/          # Records de request/response
│   │       ├── exception/    # GlobalExceptionHandler (ProblemDetail RFC 7807)
│   │       └── filter/       # RateLimitFilter
│   └── src/main/resources/
│       └── db/migration/     # Flyway V1–V8
├── frontend/
│   └── src/app/
│       ├── core/
│       │   ├── guards/       # auth, consent, subscription, featureFlag, role
│       │   ├── interceptors/ # credentials, auth-refresh, error
│       │   ├── models/       # auth, lgpd, subscription, feature-flag…
│       │   ├── services/     # Auth, Lgpd, FeatureFlag, Subscription,
│       │   │                 #   Manutencao, Veiculo, Documento, Meta…
│       │   └── validators/   # cnpj.validator
│       ├── features/
│       │   ├── auth/         # Login, Signup
│       │   ├── billing/      # Pricing (público), BillingDashboard
│       │   ├── lgpd/         # Consent, PrivacyPolicy, TermsOfUse, LgpdPanel
│       │   ├── dashboard/
│       │   ├── manutencoes/
│       │   ├── veiculos/
│       │   ├── documentos/
│       │   ├── metas/        # Mecânico, Gerente, Detalhe
│       │   ├── financeiro/
│       │   ├── relatorios/
│       │   ├── admin/        # FeatureFlags (toggle inline), Usuarios
│       │   ├── rastreio/
│       │   └── errors/       # 403
│       └── shared/
│           └── components/layout/  # ShellComponent (sidebar + topbar)
├── scripts/
│   └── set-env.js            # Injeta VITE_API_URL em environment.prod.ts no build
└── docker-compose.yml
```

---

## Licença e Direitos Autorais

```
Copyright (c) 2025 RiseCode Studio
Todos os direitos reservados.
```

Este software e todo o seu código-fonte, design, arquitetura e documentação são propriedade exclusiva da **RiseCode Studio** e estão protegidos pelas leis de direitos autorais do Brasil (Lei nº 9.610/1998) e pelos tratados internacionais de propriedade intelectual.

**É expressamente proibido**, sem autorização prévia e por escrito da RiseCode Studio:

- Copiar, reproduzir ou redistribuir o código-fonte, no todo ou em parte
- Modificar, traduzir ou criar obras derivadas
- Usar o software ou qualquer parte dele para fins comerciais sem licença
- Fazer engenharia reversa, descompilar ou desmontar o software
- Remover ou alterar avisos de direitos autorais, marcas ou atribuições

**Contato para licenciamento:**
[contato@risecode.studio](mailto:contato@risecode.studio)

---

<p align="center">
  Desenvolvido com ♥ por <strong>RiseCode Studio</strong><br>
  <a href="https://managerpitstop.com.br">managerpitstop.com.br</a>
</p>
