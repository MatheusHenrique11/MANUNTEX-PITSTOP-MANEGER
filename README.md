<h1 align="center">
  <br>
  Manager PitStop
  <br>
</h1>

<p align="center">
  Plataforma SaaS multi-tenant para gestão completa de oficinas automotivas —<br>
  ordens de serviço, frotas, documentos, faturamento e conformidade LGPD em um único lugar.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring Boot-3.3.2-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Angular-17-DD0031?style=flat-square&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/LGPD-Compliant-009c3b?style=flat-square" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" />
</p>

---

## Visão Geral

O **Manager PitStop** é um SaaS B2B de gestão para oficinas mecânicas. Cada empresa opera em seu próprio contexto completamente isolado (multi-tenant), com controle de acesso por perfil (RBAC), criptografia de ponta a ponta nos documentos, sistema de assinaturas com emissão automática de NFS-e e conformidade integral com a **Lei Geral de Proteção de Dados (LGPD — Lei 13.709/2018)**.

---

## Módulos e Funcionalidades

### Gestão Operacional

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Ordens de Serviço** | Abertura, atribuição a mecânico, acompanhamento de status (Aberta → Em Andamento → Concluída) e geração de relatório em PDF | `MAINTENANCE_MODULE` |
| **Rastreio Público** | Link público com token único para o cliente acompanhar sua OS sem login | — |
| **Gestão de Veículos** | Cadastro com placa, chassi (mascarado), RENAVAM (mascarado), marca, modelo e histórico completo de serviços | `VEHICLE_MANAGEMENT` |
| **Clientes** | Cadastro com CPF/CNPJ, telefone, e-mail e vinculação a múltiplos veículos; CPF mascarado por perfil | — |
| **Cofre de Documentos** | Upload, visualização e controle de validade de CRLV, laudos e demais documentos com criptografia AES-256-GCM | `DOCUMENT_VAULT` |

### Gestão de Pessoas e Metas

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Metas por Mecânico** | Definição de metas mensais de faturamento por mecânico, acompanhamento em tempo real e exportação de relatório PDF para RH | `GOALS_MODULE` |
| **Gestão de Usuários** | Criação, ativação/desativação e alteração de perfil (GERENTE, MECÂNICO, RECEPCIONISTA) com isolamento por empresa | — |

### Financeiro e Faturamento

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Financeiro** | Visão financeira consolidada das ordens de serviço concluídas por período | `FINANCIAL_MODULE` |
| **Assinaturas (Billing)** | Planos Starter / Professional / Enterprise via Stripe; trial de 14 dias; webhook de pagamento com validação HMAC-SHA256 | — |
| **NFS-e Automática** | Emissão automática de Nota Fiscal de Serviço Eletrônica via Focus NFe a cada pagamento confirmado; idempotência via `gatewayInvoiceId` | — |
| **Dashboard de Faturamento** | Histórico de faturas com download de PDF/XML das NFS-e emitidas | — |

### Analítico e Relatórios

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Dashboard** | KPIs e métricas consolidadas: OS abertas, em andamento, concluídas e valor total | — |
| **Relatórios** | Análises por período, mecânico e tipo de serviço | `ANALYTICS_DASHBOARD` |

### Configuração e Administração

| Módulo | Descrição | Perfil |
|---|---|---|
| **Controle de Módulos** | Ativação e desativação de feature flags por empresa em tempo real | `ROLE_ADMIN` |
| **Gestão de Empresas** | Cadastro de novos tenants com dados fiscais completos via painel admin | `ROLE_ADMIN` |
| **Auto-cadastro (Signup)** | Formulário público de auto-cadastro com validação de CNPJ (dígitos verificadores) e preenchimento automático via BrasilAPI | — |
| **Configuração da Empresa** | Logo, CNPJ, endereço e dados fiscais para emissão de NFS-e | `ROLE_GERENTE` |

### Privacidade e LGPD

| Módulo | Descrição |
|---|---|
| **Consentimento** | Coleta e registro de consentimento por versão de política (Política de Privacidade + Termos de Uso) — Art. 8 LGPD |
| **Portal do Titular** | Interface para solicitar acesso, portabilidade, correção, anonimização e oposição ao tratamento — Art. 18 LGPD |
| **Exportação de Dados** | Download de todos os dados pessoais em formato JSON estruturado (portabilidade) |
| **Anonimização** | Substituição de PII (e-mail, nome, senha) com revogação de sessões e marcação `deleted_at` |
| **Retenção Automática** | Job diário às 03h: hard-delete de registros anonimizados após 90 dias + limpeza de tokens expirados |
| **Audit Log** | Registro imutável de todas as operações sobre dados pessoais (exportação, anonimização, consentimento) |

---

## Perfis de Acesso (RBAC)

| Perfil | Permissões |
|---|---|
| `ROLE_ADMIN` | Acesso total; gerencia todas as empresas, usuários, feature flags e DSARs globais |
| `ROLE_GERENTE` | Cria e gerencia OS, metas, usuários da própria empresa; acessa faturamento e relatórios |
| `ROLE_MECANICO` | Visualiza apenas as próprias OS e metas do mês; não acessa dados financeiros |
| `ROLE_RECEPCIONISTA` | Abre e consulta OS; sem acesso a dados financeiros, metas ou relatórios |

> Dados sensíveis (chassi, RENAVAM, CPF/CNPJ completo) são mascarados para `ROLE_MECANICO` e `ROLE_RECEPCIONISTA`.

---

## Stack Tecnológica

### Backend — Java 21 / Spring Boot 3.3.2

| Tecnologia | Uso |
|---|---|
| **Spring Security** | JWT em cookies HTTP-Only + SameSite; refresh token rotation; RBAC via `@PreAuthorize` |
| **Spring Data JPA** | PostgreSQL 16 + Hibernate 6; `@SQLRestriction` para soft-delete transparente |
| **Flyway** | Migrations versionadas (V1–V8); baseline automático |
| **Hibernate Validator** | `@CNPJ`, `@CPF`, validações customizadas de placa e chassi |
| **Bucket4j** | Rate limiting por IP (60 req/min) com header `X-RateLimit-Remaining` |
| **OpenPDF** | Relatórios mensais de metas em PDF |
| **MinIO / S3** | Armazenamento de documentos; URLs pré-assinadas com expiração de 5 min |
| **RestClient** (Spring 6.1) | Integração HTTP com BrasilAPI, Focus NFe e Stripe |
| **Jackson** | Serialização JSON; `@JsonIgnoreProperties` nos contratos externos |

### Frontend — Angular 17

| Tecnologia | Uso |
|---|---|
| **Standalone Components** | Sem `NgModule`; lazy loading por rota via `loadComponent` |
| **Signals** | Estado reativo (`signal`, `computed`) sem RxJS para dados de UI |
| **Reactive Forms** | Formulários com validadores customizados (CNPJ matemático, confirmação de senha) |
| **HttpClient + Interceptors** | Credenciais automáticas; renovação silenciosa de token expirado |
| **Guards encadeados** | `authGuard` → `consentGuard` → `subscriptionGuard` → `featureFlagGuard` → `roleGuard` |
| **Tailwind CSS** | Design system customizado: paleta `petroleum`, `safety`, `surface` |

### Infraestrutura

| Componente | Descrição |
|---|---|
| **Docker Compose** | PostgreSQL 16 + MinIO com bucket privado provisionado automaticamente |
| **Railway** | Deploy em produção via variáveis de ambiente |
| **BrasilAPI** | Consulta de dados cadastrais de CNPJ (gratuita, sem chave) |
| **Stripe** | Processamento de pagamentos; webhook validado por HMAC-SHA256 |
| **Focus NFe** | Emissão de NFS-e em ambiente de homologação e produção |

---

## Segurança

O projeto implementa o modelo **Defense in Depth** com múltiplas camadas:

### Autenticação e Sessão
- JWT em cookies `HttpOnly + Secure + SameSite=None` — nunca no `localStorage`
- Refresh tokens persistidos apenas como hash SHA-256; o token raw nunca toca o banco
- Rotação automática de refresh token a cada uso (proteção contra replay)
- Detecção de roubo de token: segundo uso do mesmo refresh token revoga toda a família

### Dados em Repouso
- Documentos cifrados com **AES-256-GCM** + IV aleatório de 12 bytes antes do upload
- `storageKey` nunca exposta ao frontend — URLs de download são pré-assinadas com expiração
- Senhas com hash **BCrypt** (custo 12)

### Proteção de API
- Rate limiting por IP em todas as rotas
- Validação de arquivos por **Magic Numbers** (não por extensão)
- HMAC-SHA256 na validação de webhooks Stripe
- Headers de segurança: CSP, `frame-ancestors 'none'`, Referrer-Policy, Permissions-Policy

### Isolamento Multi-Tenant
- `empresaId` propagado pelo JWT claim e verificado em **todas** as queries
- `@SQLRestriction` automático em entidades com soft-delete
- Sem possibilidade de acesso cross-tenant por design (não apenas por validação)

### Mascaramento de Dados
- CPF/CNPJ de clientes: exibido completo apenas para `ROLE_ADMIN` e `ROLE_GERENTE`
- Chassi e RENAVAM: apenas últimos 3–4 dígitos visíveis para mecânicos e recepcionistas

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
Preenche automaticamente razão social, endereço e dados fiscais no formulário de cadastro. Sem chave de API necessária.

### Stripe — Pagamentos
- Criação de sessão de checkout (`criarCheckout`)
- Webhook `invoice.paid` → atualiza status da assinatura e dispara emissão de NFS-e
- Webhook `customer.subscription.deleted` → suspende acesso

### Focus NFe — Nota Fiscal de Serviço
- Emissão automática após `invoice.paid` com dados completos do Tomador (endereço LGPD-ready)
- Idempotência via `gatewayInvoiceId` — sem risco de NFS-e duplicada em retentativas
- Modo mock local quando `FOCUS_NFE_TOKEN` não está configurado

---

## Como Executar

### Pré-requisitos

- Docker e Docker Compose
- Java 21 (recomendado: SDKMAN ou ASDF)
- Node.js 20+ e npm

### 1. Infraestrutura (banco + storage)

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
cp .env.example .env   # configure as variáveis necessárias
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
| `DATABASE_URL` | JDBC URL do PostgreSQL (`jdbc:postgresql://...`) |
| `JWT_SECRET` | Segredo HS256 — mínimo 256 bits (Base64) |
| `ENCRYPTION_MASTER_KEY` | Chave mestre AES-256 para documentos (Base64) |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas pelo CORS (ex: `https://app.manutex.com.br`) |

### Integrações (opcionais em desenvolvimento)

| Variável | Descrição |
|---|---|
| `STRIPE_SECRET_KEY` | Chave secreta Stripe (mock ativo quando vazio) |
| `STRIPE_WEBHOOK_SECRET` | Segredo de validação do webhook Stripe |
| `FOCUS_NFE_TOKEN` | Token da API Focus NFe (mock ativo quando vazio) |
| `FOCUS_NFE_API_URL` | URL da API Focus NFe (padrão: homologação) |
| `NFE_MUNICIPIO_PRESTADOR` | Código IBGE do município do prestador |
| `STORAGE_ENDPOINT` | Endpoint S3-compatível (MinIO em dev) |
| `STORAGE_BUCKET` | Nome do bucket (padrão: `pitstop-docs`) |
| `FRONTEND_URL` | URL do frontend para redirecionamentos de pagamento |

---

## Testes

### Backend (217 testes)

```bash
cd backend
./mvnw test
```

Camadas cobertas:
- **Serviços** — `@ExtendWith(MockitoExtension.class)` com cenários de regras de negócio
- **Controllers** — `@WebMvcTest` com cenários de autorização por perfil
- **Filtros de segurança** — Rate limiting, JWT, validação de entrada

### Frontend

```bash
cd frontend
npm test
```

Cobertura: serviços HTTP (`HttpClientTestingModule`) e componentes standalone (Karma + Jasmine).

---

## Migrações do Banco

| Versão | Descrição |
|---|---|
| **V1** | Schema inicial — `users`, `clientes`, `veiculos`, `documentos`, `manutencoes`, `refresh_tokens`, `feature_toggles` |
| **V2** | Campos de manutenção estendidos + tabela `empresa_config` (logo, configurações) |
| **V3** | Coluna `tracking_token` em `manutencoes` para rastreio público |
| **V4** | Suporte multi-tenant — tabela `empresas` + coluna `empresa_id` em `users` e `clientes` |
| **V5** | Metas por mecânico — tabela `metas_mecanico` |
| **V6** | Billing — tabelas `assinaturas` e `faturas_nfe`; campos de assinatura em `empresas` |
| **V7** | Dados fiscais em `empresas` (razão social, endereço completo, e-mail e telefone fiscal para NFS-e) |
| **V8** | LGPD — `deleted_at` em `users`/`clientes`; tabelas `user_consents`, `data_subject_requests`, `audit_logs` |

---

## Estrutura do Projeto

```
Manutex-PitStop-Manager/
├── backend/
│   ├── src/main/java/com/manutex/pitstop/
│   │   ├── config/          # SecurityConfig, BillingProperties, JpaAuditing
│   │   ├── domain/
│   │   │   ├── entity/      # User, Cliente, Veiculo, Empresa, Assinatura, FaturaNfe,
│   │   │   │                #   UserConsent, DataSubjectRequest, AuditLog…
│   │   │   ├── enums/       # UserRole, StatusManutencao, SubscriptionStatus,
│   │   │   │                #   ConsentType, DsarType, DsarStatus…
│   │   │   └── repository/  # Spring Data JPA repositories
│   │   ├── security/        # JwtService, JwtAuthenticationFilter, TenantContext
│   │   ├── service/         # Regras de negócio (LgpdService, BillingService,
│   │   │                    #   FocusNfeService, CnpjLookupService, DataRetentionService…)
│   │   └── web/
│   │       ├── controller/  # REST endpoints (AuthController, LgpdController,
│   │       │                #   BillingController, WebhookController, TenantController…)
│   │       ├── dto/         # Request/Response records
│   │       ├── exception/   # GlobalExceptionHandler (ProblemDetail RFC 7807)
│   │       └── filter/      # RateLimitFilter
│   └── src/main/resources/
│       └── db/migration/    # Flyway V1–V8
├── frontend/
│   └── src/app/
│       ├── core/
│       │   ├── guards/      # authGuard, consentGuard, subscriptionGuard,
│       │   │                #   featureFlagGuard, roleGuard
│       │   ├── interceptors/ # auth-refresh, credentials
│       │   ├── models/      # lgpd.model, subscription.model, auth.model…
│       │   ├── services/    # AuthService, LgpdService, SubscriptionService,
│       │   │                #   TenantService, BillingService…
│       │   └── validators/  # cnpj.validator
│       ├── features/
│       │   ├── auth/        # LoginComponent, SignupComponent
│       │   ├── billing/     # PricingComponent, BillingDashboardComponent
│       │   ├── lgpd/        # ConsentComponent, PrivacyPolicyComponent,
│       │   │                #   TermsOfUseComponent, LgpdPanelComponent
│       │   ├── dashboard/
│       │   ├── manutencoes/
│       │   ├── veiculos/
│       │   ├── documentos/
│       │   ├── metas/
│       │   ├── admin/
│       │   └── errors/
│       └── shared/
│           └── components/layout/  # ShellComponent (sidebar + topbar)
└── docker-compose.yml
```

---

## Licença

Proprietário — todos os direitos reservados © Manutex Tecnologia Ltda.
