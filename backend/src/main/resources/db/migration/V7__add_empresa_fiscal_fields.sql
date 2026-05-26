-- V7: Dados fiscais do tenant (necessários para emissão de NFS-e como Tomador)

ALTER TABLE empresas
    ADD COLUMN IF NOT EXISTS razao_social            VARCHAR(200),
    ADD COLUMN IF NOT EXISTS nome_fantasia           VARCHAR(200),
    ADD COLUMN IF NOT EXISTS cep                     VARCHAR(10),
    ADD COLUMN IF NOT EXISTS logradouro              VARCHAR(200),
    ADD COLUMN IF NOT EXISTS numero                  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS complemento             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bairro                  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cidade                  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS uf                      CHAR(2),
    ADD COLUMN IF NOT EXISTS codigo_municipio_ibge   VARCHAR(10),
    ADD COLUMN IF NOT EXISTS email_fiscal            VARCHAR(180),
    ADD COLUMN IF NOT EXISTS telefone_fiscal         VARCHAR(20);

-- cnpj já existe e é único; garante que o campo aceite ambos os formatos
COMMENT ON COLUMN empresas.cnpj IS 'CNPJ no formato XX.XXX.XXX/XXXX-XX ou 14 dígitos';
