# Deploy — PitStop Manager (VPS Hostinger)

Deploy cirúrgico do backend na mesma VPS do RiggingCheck, sem derrubar nenhum serviço existente.

---

## Diagnóstico da Stack

| Item | Valor |
|------|-------|
| Runtime | Java 21 (eclipse-temurin:21-jre-alpine) |
| Framework | Spring Boot 3.3.2 |
| Build | Maven 3.9 (multi-stage Docker) |
| Banco (prod) | PostgreSQL 15 — container `risecode_postgres` |
| Banco (dev) | `pitstop_dev` (docker-compose.yml local) |
| Porta interna | **8080** |
| Flyway | 8 migrações (V1–V8) |
| Storage | AWS S3 / Cloudflare R2 (MinIO só em dev) |

---

## Infraestrutura Alvo

```
VPS 2.25.151.136
├── Traefik (já rodando)
├── risecode_postgres   ← rede: risecode_default
├── riggingcheck_backend ← rede: riggingcheck_network
└── pitstopmanager_backend  ← NOVO
        ├── rede: pitstopmanager_network (interna)
        ├── rede: risecode_default  (acesso ao postgres + Traefik)
        └── rede: riggingcheck_network (fallback Traefik)
```

---

## Passo 0 — Pré-requisitos

### DNS
Antes de qualquer coisa, crie o registro A no painel de DNS:

```
api.pitstopmanager.com  →  2.25.151.136
```

Aguarde propagação (geralmente 1–5 min na Hostinger).

### Verificar redes existentes na VPS
```bash
docker network ls
# Confirme que existem: risecode_default, riggingcheck_network
```

> **Atenção sobre o Traefik:**  
> Se o Traefik estiver apenas na `riggingcheck_network` (e não na `risecode_default`),
> edite o `docker-compose.prod.yml` e remova o bloco `risecode_default` do serviço,
> mantendo apenas `riggingcheck_network`. Use o label:
> `traefik.docker.network=riggingcheck_network`

---

## Passo 1 — Criar banco e usuário PostgreSQL

Execute **dentro do container** do PostgreSQL existente.  
**Não** expõe a porta do Postgres para a internet.

```bash
# Abre o psql no container
docker exec -it risecode_postgres psql -U postgres

# Cola os comandos abaixo (um bloco por vez):
```

```sql
-- Criar usuário (se já existir, ignorar o erro e continuar)
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'pitstopmanager_user') THEN
    CREATE USER pitstopmanager_user WITH PASSWORD 'SENHA_FORTE_AQUI';
  ELSE
    RAISE NOTICE 'Usuário pitstopmanager_user já existe — nenhuma ação.';
  END IF;
END
$$;

-- Criar banco (se já existir, ignorar)
SELECT 'CREATE DATABASE pitstopmanager_db OWNER pitstopmanager_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pitstopmanager_db')\gexec

-- Permissões
GRANT ALL PRIVILEGES ON DATABASE pitstopmanager_db TO pitstopmanager_user;

-- Conectar ao banco recém-criado e garantir permissão no schema public
\c pitstopmanager_db
GRANT ALL ON SCHEMA public TO pitstopmanager_user;

-- Verificar
\l pitstopmanager_db
\q
```

Gere a senha forte antes de executar:
```bash
openssl rand -base64 32
```

---

## Passo 2 — Clonar / atualizar o repositório na VPS

```bash
# Se for o primeiro deploy:
cd /opt
git clone https://github.com/SEU_USUARIO/Manutex-PitStop-Manager.git pitstop
cd /opt/pitstop

# Se for atualização:
cd /opt/pitstop
git pull origin main
```

---

## Passo 3 — Configurar variáveis de ambiente

```bash
cd /opt/pitstop
cp .env.production.example .env.production
nano .env.production   # ou vim
```

Valores obrigatórios a preencher:

```bash
# Gerar JWT_SECRET
openssl rand -base64 64

# Gerar ENCRYPTION_MASTER_KEY
openssl rand -base64 32

# Gerar ENCRYPTION_SALT
openssl rand -base64 16
```

Preencher `DB_PASSWORD` com a mesma senha gerada no Passo 1.

---

## Passo 4 — Buildar a imagem Docker

O build usa multi-stage (Maven → JRE). Executar da **raiz do projeto**:

```bash
cd /opt/pitstop

docker build \
  -f backend/Dockerfile \
  -t pitstopmanager/backend:latest \
  .
```

> O build leva ~3–5 min na primeira vez (download de dependências Maven).  
> Builds subsequentes usam cache do Docker.

---

## Passo 5 — Validar o compose antes de subir

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production config
```

Confirmar que:
- `DB_URL` aponta para `risecode_postgres:5432/pitstopmanager_db`
- `CORS_ALLOWED_ORIGINS` = `https://pitstopmanager.com`
- Sem variáveis em branco obrigatórias

---

## Passo 6 — Subir o container

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
```

---

## Passo 7 — Acompanhar os logs (startup + Flyway)

```bash
docker logs -f pitstopmanager_backend
```

Aguardar a linha:
```
Started PitstopBackendApplication in X.XXX seconds
```

O Flyway executará as 8 migrações automaticamente no primeiro boot.

---

## Passo 8 — Validar saúde e HTTPS

```bash
# Health via HTTP interno (dentro da VPS)
curl http://localhost:8080/actuator/health   # deve retornar {"status":"UP"}

# Health via HTTPS público (após Traefik emitir certificado)
curl https://api.pitstopmanager.com/actuator/health
```

```bash
# Testar o endpoint de login (POST)
curl -s -X POST https://api.pitstopmanager.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@teste.com","password":"Teste@1234"}' | jq .
```

```bash
# Verificar certificado Let's Encrypt
curl -I https://api.pitstopmanager.com/actuator/health
# Deve mostrar: HTTP/2 200, issuer: R3 (Let's Encrypt)
```

---

## Passo 9 — Configurar o Frontend na Vercel

No painel da Vercel, adicionar as variáveis de ambiente do projeto PitStop Manager:

| Variável | Valor |
|----------|-------|
| `VITE_API_URL` | `https://api.pitstopmanager.com/api` |
| `VITE_FRONTEND_URL` | `https://pitstopmanager.com` |

Após salvar, fazer redeploy do frontend.

---

## Passo 10 — Ativar backup automático

```bash
# Copiar o script para a VPS e tornar executável
chmod +x /opt/pitstop/backup-pitstop.sh

# Testar manualmente
BACKUP_DIR=/opt/pitstop/backups ./backup-pitstop.sh

# Adicionar ao cron (backup todo domingo às 02:00)
crontab -e
# Adicionar a linha:
# 0 2 * * 0 /opt/pitstop/backup-pitstop.sh >> /var/log/pitstop-backup.log 2>&1
```

---

## Rollback

Se algo der errado, apenas remova o container do PitStop. O RiggingCheck e o Postgres não são afetados:

```bash
docker compose -f docker-compose.prod.yml down
# (sem -v — nunca apague volumes)
```

Para restaurar um backup:
```bash
# Listar backups
ls /opt/pitstop/backups/

# Restaurar (substitua o arquivo pelo desejado)
gunzip -c /opt/pitstop/backups/pitstopmanager_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i risecode_postgres psql -U pitstopmanager_user -d pitstopmanager_db
```

---

## Arquivos criados por este deploy

| Arquivo | Descrição |
|---------|-----------|
| `backend/src/main/resources/application-prod.yml` | Profile Spring de produção |
| `docker-compose.prod.yml` | Compose de produção (sem Postgres, com Traefik labels) |
| `.env.production.example` | Template das variáveis de ambiente |
| `backup-pitstop.sh` | Script de backup com retenção de 30 dias |
| `DEPLOY.md` | Este documento |

Arquivos **não alterados**: `docker-compose.yml` (dev), `backend/Dockerfile`, `.dockerignore`, regras de negócio, frontend.

---

## Critérios de aceite

- [ ] RiggingCheck continua no ar após o deploy
- [ ] `risecode_postgres` continua no ar
- [ ] `pitstopmanager_backend` responde em `http://localhost:8080/actuator/health`
- [ ] `https://api.pitstopmanager.com/actuator/health` retorna `{"status":"UP"}`
- [ ] Certificado Let's Encrypt emitido (HTTPS sem aviso)
- [ ] Flyway executou as 8 migrações sem erro
- [ ] Banco `pitstopmanager_db` conectado e isolado do RiggingCheck
- [ ] CORS permite `https://pitstopmanager.com`
- [ ] Frontend na Vercel consegue chamar a API (login funcional)
- [ ] Backup manual executado com sucesso

---

## Riscos restantes

| Risco | Mitigação |
|-------|-----------|
| Traefik não descobre o container | Verificar em qual rede (`risecode_default` vs `riggingcheck_network`) o Traefik está; ajustar o label `traefik.docker.network` |
| Porta 8080 já em uso na VPS | `ss -tlnp | grep 8080` — o container usa rede interna, sem bind de porta |
| Flyway detecta checksum diferente | Nunca editar migrações já aplicadas em prod |
| Storage S3 não configurado | Documentos não serão salvos; configure `AWS_*` antes de testar upload |
| `pitstopmanager_db` já existe | O SQL usa `IF NOT EXISTS` — seguro re-executar |
| Memória insuficiente | JVM configurada para 256–512 MB (`JAVA_OPTS` no Dockerfile); verificar com `free -h` |
