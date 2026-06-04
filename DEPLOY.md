# Deploy — Manager PitStop (VPS Hostinger)

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
| Flyway | 10 migrações (V1–V10) |
| Storage | AWS S3 / Cloudflare R2 (MinIO só em dev) |

---

## Infraestrutura Alvo

```
VPS 2.25.151.136
├── Traefik            ← rede: riggingcheck_network
├── risecode_postgres  ← rede: risecode_default  (admin: risecode_admin)
├── riggingcheck_backend ← rede: riggingcheck_network
└── managerpitstop_backend  ← NOVO
        ├── rede: managerpitstop_network (interna — isolamento)
        ├── rede: risecode_default  (acesso ao postgres)
        └── rede: riggingcheck_network (Traefik descobre aqui)
```

---

## Passo 0 — Pré-requisitos

### DNS
Antes de qualquer coisa, crie os registros A no painel de DNS:

```
api.managerpitstop.com.br  →  2.25.151.136
managerpitstop.com.br      →  IP da Vercel (frontend)
```

Aguarde propagação (geralmente 1–5 min na Hostinger).

### Verificar redes existentes na VPS
```bash
docker network ls
# Deve listar: risecode_default, riggingcheck_network
```

---

## Passo 1 — Criar banco e usuário PostgreSQL

Execute **dentro do container** do PostgreSQL existente.
**Não** expõe a porta do Postgres para a internet.

```bash
# Abre o psql no container com o admin real da VPS
docker exec -it risecode_postgres psql -U risecode_admin
```

```sql
CREATE USER managerpitstop_user WITH PASSWORD 'SENHA_FORTE_DO_PITSTOP';
CREATE DATABASE managerpitstop_db OWNER managerpitstop_user;
GRANT ALL PRIVILEGES ON DATABASE managerpitstop_db TO managerpitstop_user;
\c managerpitstop_db
GRANT ALL ON SCHEMA public TO managerpitstop_user;

-- Verificar
\l managerpitstop_db
\q
```

> Se o usuário ou banco já existirem, o PostgreSQL retornará um erro (ex: `ERROR: role already exists`).
> Nesse caso basta pular o comando que falhou e continuar com os GRANTs.

---

## Passo 2 — Clonar / atualizar o repositório na VPS

```bash
# Se for o primeiro deploy:
cd /opt
git clone https://github.com/SEU_USUARIO/Manutex-PitStop-Manager.git managerpitstop
cd /opt/managerpitstop

# Se for atualização:
cd /opt/managerpitstop
git pull origin main
```

---

## Passo 3 — Configurar variáveis de ambiente

```bash
cd /opt/managerpitstop
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

Preencher `DB_PASSWORD` e `SPRING_DATASOURCE_PASSWORD` com a mesma senha gerada no Passo 1.

---

## Passo 4 — Buildar a imagem Docker

O build usa multi-stage (Maven → JRE). Executar da **raiz do projeto**:

```bash
cd /opt/managerpitstop

docker build \
  -f backend/Dockerfile \
  -t managerpitstop/backend:latest \
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
- `DB_URL` aponta para `risecode_postgres:5432/managerpitstop_db`
- `CORS_ALLOWED_ORIGINS` = `https://managerpitstop.com.br`
- Sem variáveis em branco obrigatórias

---

## Passo 6 — Subir o container

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
```

---

## Passo 7 — Acompanhar os logs (startup + Flyway)

```bash
docker logs -f managerpitstop_backend
```

Aguardar a linha:
```
Started PitstopBackendApplication in X.XXX seconds
```

O Flyway executará as 10 migrações (V1–V10) automaticamente no primeiro boot.
Verifique também os avisos do `ProductionReadinessValidator`:

```
# OK — pronto para produção:
[PITSTOP] Validação de produção: todas as configurações OK.

# Aviso — algo faltando (não impede o start):
⚠  STRIPE_WEBHOOK_SECRET ausente — webhook aceita sem validação...

# Erro fatal — impede o start:
✗  FOCUS_NFE_TOKEN de produção configurado mas CNPJ da RiseCode Studio ausente.
```

---

## Passo 8 — Validar saúde e HTTPS

```bash
# 1. Health via HTTP interno (dentro da VPS — valida que o container subiu)
curl -i http://localhost:8080/actuator/health
# Esperado: HTTP/1.1 200  {"status":"UP"}

# 2. Health via HTTPS público (valida Traefik + Let's Encrypt)
curl -i https://api.managerpitstop.com.br/actuator/health
# Esperado: HTTP/2 200  {"status":"UP"}
# Header: issuer=R3 (Let's Encrypt)

# 3. Testar endpoint de login
curl -i https://api.managerpitstop.com.br/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@teste.com","password":"123456"}'
# Esperado: HTTP/2 401 (credenciais inválidas = API está respondendo)
# ou HTTP/2 200 com accessToken se as credenciais forem válidas

# 4. Verificar homepage do frontend (Vercel)
curl -I https://managerpitstop.com.br
# Esperado: HTTP/2 200
```

---

## Passo 9 — Configurar o Frontend na Vercel

O Angular usa `environment.prod.ts` com a `apiUrl` hardcoded — **não são necessárias variáveis de ambiente no painel da Vercel** para o funcionamento básico.

A URL da API já está definida em [frontend/src/environments/environment.prod.ts](frontend/src/environments/environment.prod.ts):
```ts
apiUrl: 'https://api.managerpitstop.com.br/api/v1'
```

Se precisar alterar o domínio da API sem fazer commit, configure via Vercel Dashboard:

| Variável | Quando usar |
|----------|-------------|
| _(nenhuma obrigatória)_ | O build usa `environment.prod.ts` diretamente |

Após qualquer mudança no frontend, o Vercel faz redeploy automático via git push para `main`.

---

## Passo 10 — Ativar backup automático

```bash
chmod +x /opt/managerpitstop/backup-pitstop.sh

# Testar manualmente
BACKUP_DIR=/opt/managerpitstop/backups ./backup-pitstop.sh

# Adicionar ao cron (backup todo domingo às 02:00)
crontab -e
# Adicionar a linha:
# 0 2 * * 0 /opt/managerpitstop/backup-pitstop.sh >> /var/log/managerpitstop-backup.log 2>&1
```

---

## Rollback

Se algo der errado, apenas remova o container do Manager PitStop. O RiggingCheck e o Postgres não são afetados:

```bash
docker compose -f docker-compose.prod.yml down
# (sem -v — nunca apague volumes)
```

Para restaurar um backup:
```bash
# Listar backups
ls /opt/managerpitstop/backups/

# Restaurar (substitua o arquivo pelo desejado)
gunzip -c /opt/managerpitstop/backups/managerpitstop_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i risecode_postgres psql -U managerpitstop_user -d managerpitstop_db
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
- [ ] `managerpitstop_backend` responde em `http://localhost:8080/actuator/health`
- [ ] `https://api.managerpitstop.com.br/actuator/health` retorna `{"status":"UP"}`
- [ ] Certificado Let's Encrypt emitido (HTTPS sem aviso)
- [ ] Flyway executou as 10 migrações (V1–V10) sem erro
- [ ] Banco `managerpitstop_db` conectado e isolado do RiggingCheck
- [ ] CORS permite `https://managerpitstop.com.br`
- [ ] Frontend na Vercel consegue chamar a API (login funcional)
- [ ] Backup manual executado com sucesso

---

## Riscos restantes

| Risco | Mitigação |
|-------|-----------|
| Traefik não descobre o container | Confirmar que o label `traefik.docker.network=riggingcheck_network` está correto; inspecionar com `docker inspect traefik` |
| Porta 8080 já em uso na VPS | `ss -tlnp | grep 8080` — o container usa rede interna, sem bind de porta |
| Flyway detecta checksum diferente | Nunca editar migrações já aplicadas em prod |
| Storage S3 não configurado | Documentos não serão salvos; configure `AWS_*` antes de testar upload |
| `managerpitstop_db` já existe | Pular o `CREATE` que falhou e executar apenas os `GRANT`s |
| Memória insuficiente | JVM configurada para 256–512 MB (`JAVA_OPTS` no Dockerfile); verificar com `free -h` |
