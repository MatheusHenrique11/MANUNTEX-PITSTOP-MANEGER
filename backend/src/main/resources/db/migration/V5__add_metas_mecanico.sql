-- ============================================================
-- V5 — Módulo de Metas por Mecânico
-- ============================================================

CREATE TABLE metas_mecanico (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    mecanico_id   UUID         NOT NULL REFERENCES users(id),
    empresa_id    UUID         NOT NULL REFERENCES empresas(id),
    mes           SMALLINT     NOT NULL CHECK (mes BETWEEN 1 AND 12),
    ano           SMALLINT     NOT NULL CHECK (ano >= 2020),
    valor_meta    NUMERIC(12,2) NOT NULL CHECK (valor_meta > 0),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(180),
    updated_by    VARCHAR(180),

    CONSTRAINT uq_meta_mecanico_periodo UNIQUE (mecanico_id, mes, ano)
);

CREATE INDEX idx_metas_mecanico_id ON metas_mecanico(mecanico_id);
CREATE INDEX idx_metas_empresa_id  ON metas_mecanico(empresa_id);
CREATE INDEX idx_metas_periodo     ON metas_mecanico(ano, mes);
