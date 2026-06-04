-- ══════════════════════════════════════════════════════════════════════════════
-- V10 — Configuração Fiscal da Plataforma (RiseCode Studio) e por Tenant
--
-- Fluxo A: RiseCode Studio emite NFS-e para a oficina (assinatura SaaS)
--   Prestador = platform_fiscal_config
--   Tomador   = empresa + tenant_fiscal_config
--
-- Fluxo B: Oficina emite NFS-e para o cliente final
--   Prestador = tenant_fiscal_config
--   Tomador   = clientes
-- ══════════════════════════════════════════════════════════════════════════════

-- ── Dados fiscais da plataforma RiseCode Studio ───────────────────────────────
-- Tabela de linha única: a plataforma tem apenas um perfil fiscal.
CREATE TABLE IF NOT EXISTS platform_fiscal_config (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    razao_social                VARCHAR(200) NOT NULL,
    nome_fantasia               VARCHAR(200),
    cnpj                        VARCHAR(18)  NOT NULL,
    inscricao_municipal         VARCHAR(30),
    regime_tributario           VARCHAR(30),
    codigo_municipio            VARCHAR(10)  NOT NULL,
    municipio                   VARCHAR(100),
    uf                          CHAR(2),
    endereco                    VARCHAR(200),
    numero                      VARCHAR(20),
    bairro                      VARCHAR(100),
    cep                         VARCHAR(10),
    email_fiscal                VARCHAR(180),
    telefone_fiscal             VARCHAR(30),
    codigo_servico_municipal    VARCHAR(30),
    item_lista_servico          VARCHAR(10),
    aliquota_iss                NUMERIC(5,2) NOT NULL DEFAULT 2.00,
    ambiente_fiscal             VARCHAR(20)  NOT NULL DEFAULT 'HOMOLOGACAO',
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by                  VARCHAR(180)
);

COMMENT ON TABLE platform_fiscal_config IS 'Dados fiscais da RiseCode Studio para emissão de NFS-e da assinatura SaaS. Apenas ROLE_ADMIN pode alterar.';
COMMENT ON COLUMN platform_fiscal_config.ambiente_fiscal IS 'HOMOLOGACAO | PRODUCAO';

-- ── Dados fiscais por tenant (oficina assinante) ──────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_fiscal_config (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id                  UUID         NOT NULL UNIQUE REFERENCES empresas(id) ON DELETE CASCADE,
    razao_social                VARCHAR(200),
    nome_fantasia               VARCHAR(200),
    cnpj                        VARCHAR(18),
    inscricao_municipal         VARCHAR(30),
    regime_tributario           VARCHAR(30),
    codigo_municipio            VARCHAR(10),
    municipio                   VARCHAR(100),
    uf                          CHAR(2),
    endereco                    VARCHAR(200),
    numero                      VARCHAR(20),
    bairro                      VARCHAR(100),
    cep                         VARCHAR(10),
    email_fiscal                VARCHAR(180),
    telefone_fiscal             VARCHAR(30),
    codigo_servico_municipal    VARCHAR(30),
    item_lista_servico          VARCHAR(10),
    aliquota_iss                NUMERIC(5,2) NOT NULL DEFAULT 2.00,
    ambiente_fiscal             VARCHAR(20)  NOT NULL DEFAULT 'HOMOLOGACAO',
    fiscal_enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
    fiscal_validated_at         TIMESTAMPTZ,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by                  VARCHAR(180)
);

COMMENT ON TABLE tenant_fiscal_config IS 'Dados fiscais de cada oficina. Prestador nas notas emitidas pela oficina para o cliente final.';
COMMENT ON COLUMN tenant_fiscal_config.fiscal_enabled IS 'TRUE somente quando os dados foram validados e a emissão está habilitada para este tenant.';

CREATE INDEX IF NOT EXISTS idx_tenant_fiscal_empresa ON tenant_fiscal_config(empresa_id);

-- ── Tipo de nota na tabela faturas_nfe ────────────────────────────────────────
-- SAAS     = RiseCode → oficina (assinatura do plano)
-- WORKSHOP = oficina → cliente final (serviços automotivos)
ALTER TABLE faturas_nfe
    ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(20) NOT NULL DEFAULT 'SAAS';

COMMENT ON COLUMN faturas_nfe.invoice_type IS 'SAAS = nota da assinatura (RiseCode → tenant). WORKSHOP = nota da oficina (tenant → cliente).';

-- ── Stripe price IDs configuráveis ───────────────────────────────────────────
-- Armazenado no application.yml via env vars; sem tabela própria.
-- Os price IDs são validados pelo ProductionReadinessValidator no startup.
