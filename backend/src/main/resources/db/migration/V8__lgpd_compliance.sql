-- ══════════════════════════════════════════════════════════════════════════════
-- V8 — LGPD Compliance (Lei Geral de Proteção de Dados — Lei 13.709/2018)
-- ══════════════════════════════════════════════════════════════════════════════

-- ── Soft-delete: usuários e clientes ─────────────────────────────────────────
ALTER TABLE users    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at    ON users(deleted_at)    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_clientes_deleted_at ON clientes(deleted_at) WHERE deleted_at IS NULL;

-- ── Consentimentos do titular (Art. 8 LGPD) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS user_consents (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    policy_type     VARCHAR(50)  NOT NULL,   -- 'PRIVACY_POLICY' | 'TERMS_OF_USE'
    policy_version  VARCHAR(20)  NOT NULL,
    accepted        BOOLEAN      NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_consents PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_user_consents_user_id ON user_consents(user_id);

-- ── Solicitações do titular (DSARs — Art. 18 LGPD) ───────────────────────────
CREATE TABLE IF NOT EXISTS data_subject_requests (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    empresa_id          UUID         NOT NULL REFERENCES empresas(id),
    requester_user_id   UUID,                  -- nullable: pode ser anonimizado
    requester_email     VARCHAR(180) NOT NULL,  -- histórico imutável
    request_type        VARCHAR(50)  NOT NULL,  -- ACCESS | ERASURE | CORRECTION | PORTABILITY | OBJECTION
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    notes               TEXT,
    response_notes      TEXT,
    requested_at        TIMESTAMP    NOT NULL DEFAULT now(),
    deadline_at         TIMESTAMP    NOT NULL,  -- 15 dias úteis (LGPD)
    completed_at        TIMESTAMP,
    processed_by        VARCHAR(180),

    CONSTRAINT pk_dsr PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_dsr_empresa_id ON data_subject_requests(empresa_id);
CREATE INDEX IF NOT EXISTS idx_dsr_status     ON data_subject_requests(status);

-- ── Log de auditoria de acesso a dados pessoais (Art. 46 LGPD) ───────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL    NOT NULL,
    empresa_id      UUID         REFERENCES empresas(id) ON DELETE SET NULL,
    user_email      VARCHAR(180),
    action          VARCHAR(60)  NOT NULL,  -- DATA_EXPORT | ANONYMIZE | CONSENT | SENSITIVE_VIEW | RETENTION_CLEANUP
    resource_type   VARCHAR(60)  NOT NULL,  -- USER | CLIENTE | DOCUMENTO | DSAR
    resource_id     VARCHAR(100),
    detail          TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_empresa_id ON audit_logs(empresa_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
