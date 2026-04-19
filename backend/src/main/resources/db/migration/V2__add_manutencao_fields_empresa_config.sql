ALTER TABLE manutencoes
    ADD COLUMN IF NOT EXISTS relatorio    TEXT,
    ADD COLUMN IF NOT EXISTS orcamento   NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS valor_final NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS km_saida    INTEGER;

CREATE TABLE IF NOT EXISTS empresa_config (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(150) NOT NULL,
    cnpj            VARCHAR(18),
    endereco        TEXT,
    telefone        VARCHAR(20),
    email           VARCHAR(180),
    logo_key        TEXT,
    logo_mime_type  VARCHAR(50),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
