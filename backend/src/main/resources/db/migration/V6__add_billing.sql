-- V6: Módulo de Faturamento — Assinaturas e NFS-e

-- Adiciona campos de billing na tabela empresas
ALTER TABLE empresas
    ADD COLUMN IF NOT EXISTS subscription_status       VARCHAR(20)  NOT NULL DEFAULT 'TRIAL',
    ADD COLUMN IF NOT EXISTS subscription_plan         VARCHAR(30),
    ADD COLUMN IF NOT EXISTS subscription_id           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_gateway_customer_id VARCHAR(255);

-- Tabela de assinaturas (histórico por empresa)
CREATE TABLE IF NOT EXISTS assinaturas (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id               UUID         NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    plano                    VARCHAR(30)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    gateway_customer_id      VARCHAR(255),
    gateway_subscription_id  VARCHAR(255),
    current_period_start     TIMESTAMPTZ,
    current_period_end       TIMESTAMPTZ,
    trial_end                TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Tabela de notas fiscais de serviço (NFS-e) vinculadas a cada cobrança
CREATE TABLE IF NOT EXISTS faturas_nfe (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id          UUID         NOT NULL REFERENCES empresas(id) ON DELETE RESTRICT,
    gateway_invoice_id  VARCHAR(255) UNIQUE,
    nfe_id              VARCHAR(255),
    nfe_status          VARCHAR(30),
    valor               NUMERIC(12,2) NOT NULL,
    pdf_url             TEXT,
    xml_url             TEXT,
    issue_date          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_assinaturas_empresa ON assinaturas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_assinaturas_status  ON assinaturas(status);
CREATE INDEX IF NOT EXISTS idx_faturas_empresa     ON faturas_nfe(empresa_id);
