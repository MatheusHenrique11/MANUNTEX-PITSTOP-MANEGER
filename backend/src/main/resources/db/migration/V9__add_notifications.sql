-- ══════════════════════════════════════════════════════════════════════════════
-- V9 — Módulo de Notificações (WhatsApp / E-mail)
-- ══════════════════════════════════════════════════════════════════════════════

-- ── Templates configuráveis por empresa e evento ─────────────────────────────
CREATE TABLE notification_templates (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID         NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    evento      VARCHAR(50)  NOT NULL,
    canal       VARCHAR(20)  NOT NULL,
    titulo      VARCHAR(200),
    corpo       TEXT         NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(180),
    updated_by  VARCHAR(180),
    UNIQUE (empresa_id, evento, canal)
);

CREATE INDEX idx_notif_templates_empresa ON notification_templates(empresa_id);

-- ── Log imutável de cada envio ────────────────────────────────────────────────
CREATE TABLE notification_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id      UUID        NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    manutencao_id   UUID        REFERENCES manutencoes(id) ON DELETE SET NULL,
    cliente_id      UUID        REFERENCES clientes(id)   ON DELETE SET NULL,
    evento          VARCHAR(50) NOT NULL,
    canal           VARCHAR(20) NOT NULL,
    destinatario    VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    error_message   TEXT,
    enviado_em      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_logs_empresa    ON notification_logs(empresa_id);
CREATE INDEX idx_notif_logs_manutencao ON notification_logs(manutencao_id);
CREATE INDEX idx_notif_logs_created    ON notification_logs(created_at);

-- ── Configuração de notificações por empresa (multi-tenant) ──────────────────
CREATE TABLE empresa_notification_config (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id              UUID        NOT NULL UNIQUE REFERENCES empresas(id) ON DELETE CASCADE,
    whatsapp_provider_url   VARCHAR(500),
    whatsapp_api_token      VARCHAR(500),
    whatsapp_instance_name  VARCHAR(100),
    notification_email_from VARCHAR(180),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
