#!/usr/bin/env bash
# ================================================================
# backup-pitstop.sh — Backup do banco managerpitstop_db
# Executa via pg_dump dentro do container risecode_postgres.
# NÃO expõe o PostgreSQL externamente.
#
# Uso:
#   chmod +x backup-pitstop.sh
#   ./backup-pitstop.sh
#
# Cron semanal (às 02:00 de domingo):
#   0 2 * * 0 /opt/managerpitstop/backup-pitstop.sh >> /var/log/managerpitstop-backup.log 2>&1
# ================================================================

set -euo pipefail

# ── Configurações ─────────────────────────────────────────────
CONTAINER="risecode_postgres"
DB_NAME="managerpitstop_db"
DB_USER="managerpitstop_user"
BACKUP_DIR="${BACKUP_DIR:-/opt/managerpitstop/backups}"
RETENTION_DAYS=30

# ── Timestamp ─────────────────────────────────────────────────
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/managerpitstop_${TIMESTAMP}.sql.gz"

# ── Criar diretório de backup se não existir ──────────────────
mkdir -p "${BACKUP_DIR}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iniciando backup do banco ${DB_NAME}..."

# ── Verificar se o container está rodando ─────────────────────
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "[ERRO] Container '${CONTAINER}' não está rodando. Abortando." >&2
  exit 1
fi

# ── Executar pg_dump dentro do container ─────────────────────
docker exec "${CONTAINER}" \
  pg_dump -U "${DB_USER}" "${DB_NAME}" \
  | gzip > "${BACKUP_FILE}"

BACKUP_SIZE=$(du -sh "${BACKUP_FILE}" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup criado: ${BACKUP_FILE} (${BACKUP_SIZE})"

# ── Limpar backups antigos (acima de RETENTION_DAYS dias) ─────
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Removendo backups com mais de ${RETENTION_DAYS} dias..."
find "${BACKUP_DIR}" -name "managerpitstop_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Limpeza concluída."

# ── Listar backups disponíveis ────────────────────────────────
echo ""
echo "Backups disponíveis em ${BACKUP_DIR}:"
ls -lh "${BACKUP_DIR}"/managerpitstop_*.sql.gz 2>/dev/null || echo "(nenhum)"

echo ""
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup finalizado com sucesso."
