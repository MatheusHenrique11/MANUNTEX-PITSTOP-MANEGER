-- V4: Suporte multi-tenant via Empresa (isolamento por CNPJ)

CREATE TABLE IF NOT EXISTS empresas (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       VARCHAR(150) NOT NULL,
    cnpj       VARCHAR(18)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_empresas_cnpj UNIQUE (cnpj)
);

-- Usuários vinculados a uma empresa (NULL = ROLE_ADMIN do sistema)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS empresa_id UUID REFERENCES empresas(id) ON DELETE RESTRICT;

-- Clientes vinculados a uma empresa
ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS empresa_id UUID REFERENCES empresas(id) ON DELETE RESTRICT;

-- Remove uniqueness simples de CPF/CNPJ (em multi-tenant, a unicidade é por empresa)
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS clientes_cpf_cnpj_key;

-- Unicidade composta: mesmo CPF/CNPJ pode existir em empresas diferentes
ALTER TABLE clientes
    ADD CONSTRAINT uk_clientes_cpf_cnpj_empresa
    UNIQUE NULLS NOT DISTINCT (empresa_id, cpf_cnpj);

CREATE INDEX IF NOT EXISTS idx_users_empresa    ON users(empresa_id);
CREATE INDEX IF NOT EXISTS idx_clientes_empresa ON clientes(empresa_id);
