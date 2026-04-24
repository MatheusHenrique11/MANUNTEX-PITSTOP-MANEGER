<h1 align="center">
  <br>
  Manutex PitStop Manager
  <br>
</h1>

<p align="center">
  Plataforma SaaS para gestão de oficinas automotivas — manutenções, mecânicos, metas e documentos em um único lugar.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring Boot-3.3.2-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Angular-17-DD0031?style=flat-square&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" />
</p>

---

## Visão Geral

O **Manutex PitStop Manager** é um sistema SaaS multi-tenant para gestão completa de oficinas mecânicas. Cada empresa opera em seu próprio contexto isolado, com controle de acesso por perfil (RBAC), armazenamento seguro de documentos, acompanhamento de metas por mecânico e geração de relatórios em PDF.

---

## Funcionalidades

| Módulo | Descrição | Feature Flag |
|---|---|---|
| **Manutenções** | Abertura, acompanhamento e conclusão de ordens de serviço | `MAINTENANCE_MODULE` |
| **Veículos** | Cadastro de veículos e clientes com histórico de manutenções | `VEHICLE_MANAGEMENT` |
| **Metas por Mecânico** | Definição de metas mensais, acompanhamento de produção e relatório PDF para RH | `GOALS_MODULE` |
| **Cofre de Documentos** | Upload e visualização de documentos com criptografia AES-256-GCM | `DOCUMENT_VAULT` |
| **Dashboard Analítico** | KPIs e métricas consolidadas por período | `ANALYTICS_DASHBOARD` |
| **Financeiro** | Visão financeira das ordens concluídas | `FINANCIAL_MODULE` |
| **Notificações** | Alertas internos por evento | `NOTIFICATIONS` |
| **Integração DETRAN** | Consulta de dados veiculares | `DETRAN_INTEGRATION` |

---

## Perfis de Acesso (RBAC)

| Perfil | Permissões |
|---|---|
| `ROLE_ADMIN` | Acesso total ao sistema e painel de administração |
| `ROLE_GERENTE` | Define metas, visualiza produção de todos os mecânicos, exporta PDF, gerencia OS |
| `ROLE_MECANICO` | Visualiza apenas suas próprias OS e metas do mês corrente |
| `ROLE_RECEPCIONISTA` | Abertura e consulta de OS; sem acesso a dados financeiros ou de metas |

---

## Stack Tecnológica

### Backend
- **Java 21** + **Spring Boot 3.3.2**
- **Spring Security** — JWT em cookies HTTP-Only + SameSite=Strict
- **Spring Data JPA** — PostgreSQL 16 via Flyway (migrations versionadas)
- **Togglz** — Feature flags com painel em `/admin/toggles`
- **OpenPDF** — Geração de relatórios mensais em PDF
- **MinIO / S3** — Armazenamento de documentos com criptografia dupla (AES-256-GCM + SSE-S3)

### Frontend
- **Angular 17** — Standalone components + Signals
- **Tailwind CSS** — Design system customizado (`petroleum`, `safety`, `surface`)
- **TypeScript** — Strict mode habilitado

### Infraestrutura
- **Docker Compose** — PostgreSQL 16 + MinIO com bucket privado provisionado automaticamente

---

## Segurança

O projeto segue o modelo **Zero Leak** e adere ao OWASP Top 10:

- JWT armazenado exclusivamente em cookies HTTP-Only (nunca no `localStorage`)
- Refresh tokens persistidos como hash SHA-256 — o token raw jamais toca o banco
- Documentos cifrados com AES-256-GCM + IV aleatório por arquivo antes do upload
- `storageKey` de descifragem nunca exposta ao frontend
- Validação de arquivos por **Magic Numbers** (não por extensão)
- Mascaramento de Chassi e RENAVAM para perfis sem permissão (`ROLE_MECANICO`, `ROLE_RECEPCIONISTA`)
- Isolamento multi-tenant via `TenantContext` propagado pelo JWT claim `empresaId`
- Rate limiting por IP em todas as rotas públicas

---

## Como Executar

### Pré-requisitos

- Docker e Docker Compose
- Java 21
- Node.js 20+ e npm

### 1. Infraestrutura (banco + storage)

```bash
docker compose up -d
```

PostgreSQL disponível em `localhost:5432` e MinIO em `localhost:9000` (console: `localhost:9001`).

### 2. Backend

```bash
cd backend
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

## Testes

### Backend

```bash
cd backend
./mvnw test
```

Os testes cobrem camadas de serviço (`@ExtendWith(MockitoExtension.class)`) e controladores (`@WebMvcTest`) com cenários de autorização por perfil.

### Frontend

```bash
cd frontend
npm test
```

Testes de serviços HTTP (`HttpClientTestingModule`) e componentes standalone (Karma + Jasmine).

---

## Estrutura do Projeto

```
Manutex-PitStop-Manager/
├── backend/
│   ├── src/main/java/com/manutex/pitstop/
│   │   ├── config/          # Segurança, Togglz, auditoria
│   │   ├── domain/          # Entities, repositories, enums
│   │   ├── security/        # JWT, TenantContext, filtros
│   │   ├── service/         # Regras de negócio
│   │   └── web/             # Controllers, DTOs, exception handlers
│   └── src/main/resources/
│       └── db/migration/    # Flyway (V1–V5)
├── frontend/
│   └── src/app/
│       ├── core/            # Services, models, guards, interceptors
│       ├── features/        # Módulos funcionais (metas, veículos, manutenções…)
│       └── shared/          # Layout, componentes reutilizáveis
└── docker-compose.yml
```

---

## Migrations do Banco

| Versão | Descrição |
|---|---|
| V1 | Schema inicial — empresas, usuários, veículos, clientes |
| V2 | Módulo de manutenções e ordens de serviço |
| V3 | Cofre de documentos |
| V4 | Refresh tokens e auditoria |
| V5 | Metas por mecânico (`metas_mecanico`) |

---

## Licença

Proprietário — todos os direitos reservados © Manutex.
