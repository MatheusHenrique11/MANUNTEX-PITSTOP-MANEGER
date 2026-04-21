ALTER TABLE manutencoes
    ADD COLUMN IF NOT EXISTS tracking_token UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS idx_manutencoes_tracking_token
    ON manutencoes(tracking_token);
