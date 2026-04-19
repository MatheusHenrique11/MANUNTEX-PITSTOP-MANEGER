-- ============================================================
-- V1 — Schema inicial do PitStop Manager
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- para gen_random_uuid()

-- Tabela de usuários/funcionários
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'ROLE_MECANICO',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(180),
    updated_by    VARCHAR(180)
);

-- Tabela de clientes (donos de veículos)
CREATE TABLE clientes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    cpf_cnpj      VARCHAR(18)  NOT NULL UNIQUE,      -- armazenado mascarado/criptografado na app
    telefone      VARCHAR(20),
    email         VARCHAR(180),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(180),
    updated_by    VARCHAR(180)
);

-- Tabela de veículos
CREATE TABLE veiculos (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    placa         VARCHAR(8)   NOT NULL UNIQUE,
    chassi        VARCHAR(17)  NOT NULL UNIQUE,
    renavam       VARCHAR(11)  NOT NULL UNIQUE,
    marca         VARCHAR(60)  NOT NULL,
    modelo        VARCHAR(80)  NOT NULL,
    ano_fabricacao SMALLINT    NOT NULL,
    ano_modelo    SMALLINT     NOT NULL,
    cor           VARCHAR(40),
    cliente_id    UUID         NOT NULL REFERENCES clientes(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(180),
    updated_by    VARCHAR(180)
);

-- Tabela de documentos (armazenados criptografados no S3)
CREATE TABLE documentos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    veiculo_id        UUID         REFERENCES veiculos(id),
    cliente_id        UUID         REFERENCES clientes(id),
    tipo              VARCHAR(50)  NOT NULL,          -- ex: CRLV, CNH, SEGURO
    storage_key       TEXT         NOT NULL UNIQUE,   -- caminho no S3 (nunca exposto ao frontend)
    nome_original     VARCHAR(255) NOT NULL,
    tamanho_bytes     BIGINT       NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    checksum_sha256   VARCHAR(64)  NOT NULL,           -- integridade do arquivo
    uploaded_by       UUID         NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMPTZ                     -- documentos com prazo de validade
);

-- Tabela de manutenções
CREATE TABLE manutencoes (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    veiculo_id        UUID         NOT NULL REFERENCES veiculos(id),
    mecanico_id       UUID         NOT NULL REFERENCES users(id),
    descricao         TEXT         NOT NULL,
    km_entrada        INTEGER,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ABERTA',  -- ABERTA, EM_ANDAMENTO, CONCLUIDA
    data_entrada      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    data_conclusao    TIMESTAMPTZ,
    observacoes       TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(180),
    updated_by        VARCHAR(180)
);

-- Tabela de refresh tokens (armazenados hasheados)
CREATE TABLE refresh_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash    VARCHAR(255) NOT NULL UNIQUE,
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Tabela de feature flags (complementa Togglz com persistência em DB)
CREATE TABLE feature_toggles (
    feature_name  VARCHAR(100) PRIMARY KEY,
    enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(180)
);

-- Índices de performance e segurança
CREATE INDEX idx_veiculos_cliente   ON veiculos(cliente_id);
CREATE INDEX idx_manutencoes_veiculo ON manutencoes(veiculo_id);
CREATE INDEX idx_manutencoes_mecanico ON manutencoes(mecanico_id);
CREATE INDEX idx_documentos_veiculo ON documentos(veiculo_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id) WHERE revoked = FALSE;
CREATE INDEX idx_users_email        ON users(email);
