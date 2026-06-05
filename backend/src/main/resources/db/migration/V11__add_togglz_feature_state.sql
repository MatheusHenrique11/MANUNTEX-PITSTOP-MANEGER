-- ══════════════════════════════════════════════════════════════════════════════
-- V11 — Tabela de persistência do Togglz JDBCStateRepository
--
-- Schema extraído do bytecode de togglz-core-4.4.0.jar:SchemaUpdater
--   SchemaUpdater.CREATE : FEATURE_NAME VARCHAR(100) PK | FEATURE_ENABLED INTEGER | FEATURE_USERS VARCHAR(2000)
--   SchemaUpdater.ALTER  : ADD STRATEGY_ID VARCHAR(200) | ADD STRATEGY_PARAMS VARCHAR(2000)
--   FEATURE_USERS não é removida pelo SchemaUpdater; foi omitida pois nenhuma query do
--   JDBCStateRepository usa essa coluna (SELECT/INSERT/UPDATE usam apenas as 4 abaixo).
--   TogglzConfig usa usePostgresTextColumns=true → TEXT em vez de VARCHAR.
--   TogglzConfig usa createTable=false → SchemaUpdater não executa; Flyway gerencia tudo.
--
-- FEATURE_ENABLED usa INTEGER (não BOOLEAN): 1 = ativo, 0 = inativo
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS TOGGLZ_FEATURE_STATE (
    FEATURE_NAME    TEXT    NOT NULL,
    FEATURE_ENABLED INTEGER NOT NULL DEFAULT 0,
    STRATEGY_ID     TEXT,
    STRATEGY_PARAMS TEXT,

    CONSTRAINT pk_togglz_feature_state PRIMARY KEY (FEATURE_NAME)
);

-- ── Estados iniciais das features ────────────────────────────────────────────
-- Baseado em AppFeatures.java + PlanLimits.java:
--   VEHICLE_MANAGEMENT e DOCUMENT_VAULT têm @EnabledByDefault → ativados por padrão
--   MAINTENANCE_MODULE está no plano STARTER → ativado por padrão para novos tenants
--   As demais ficam desativadas e são ativadas via PlanEnforcementService após pagamento

INSERT INTO TOGGLZ_FEATURE_STATE (FEATURE_NAME, FEATURE_ENABLED)
VALUES
    ('VEHICLE_MANAGEMENT',  1),
    ('DOCUMENT_VAULT',      1),
    ('MAINTENANCE_MODULE',  1),
    ('ANALYTICS_DASHBOARD', 0),
    ('NOTIFICATIONS',       0),
    ('FINANCIAL_MODULE',    0),
    ('DETRAN_INTEGRATION',  0),
    ('GOALS_MODULE',        0)
ON CONFLICT (FEATURE_NAME) DO NOTHING;
