#!/bin/bash
# Provisioning базы данных компании (Вариант 2 — БД на компанию).
# Создаёт sv_company_<id>, копирует DDL схемы svschema из мастер-БД,
# переносит строки компании (по company_id) и регистрирует БД в svschema.tenant_databases.
#
# Использование:
#   ./scripts/provision-company-db.sh <company_id> [<company_id> ...]
#   ./scripts/provision-company-db.sh --all
#
# Скрипт идемпотентен: данные загружаются только в пустые таблицы.
set -euo pipefail

COMPOSE_FILE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MASTER_DB="${MASTER_DB:-svdb}"
SCHEMA="${SCHEMA:-svschema}"
PG_USER="${PG_USER:-postgres}"
PG_PASS="${POSTGRES_PASSWORD:-12345}"
# Хост доступный ИЗНУТРИ контейника postgres для dblink
DBLINK_HOST="${DBLINK_HOST:-127.0.0.1}"
DBLINK_PORT="${DBLINK_PORT:-5432}"

# Таблицы прикладных данных в схеме public (comment-service)
PUBLIC_TABLES=(comments comment_replies comment_images reply_images images notifications)
PUBLIC_TABLE_LIST="'comments','comment_replies','comment_images','reply_images','images','notifications'"

cd "$COMPOSE_FILE_DIR"

if [ "${1:-}" = "--all" ]; then
  mapfile -t COMPANIES < <(docker compose exec -T postgres psql -U "$PG_USER" -d "$MASTER_DB" -Atc \
    "SELECT id FROM $SCHEMA.companies ORDER BY id")
else
  COMPANIES=("$@")
fi

[ ${#COMPANIES[@]} -gt 0 ] || { echo "Не указано ни одной компании" >&2; exit 1; }

for CID in "${COMPANIES[@]}"; do
  DB="sv_company_${CID}"
  echo "=== Company $CID -> $DB ==="

  # 1. База данных
  EXISTS=$(docker compose exec -T postgres psql -U "$PG_USER" -d "$MASTER_DB" -Atc \
    "SELECT 1 FROM pg_database WHERE datname='$DB'")
  if [ -z "$EXISTS" ]; then
    docker compose exec -T postgres psql -U "$PG_USER" -d postgres -c "CREATE DATABASE $DB"
  else
    echo "Database $DB already exists"
  fi

  # 2. Схема (только если в базе ещё нет таблиц схемы)
  TABLES=$(docker compose exec -T postgres psql -U "$PG_USER" -d "$DB" -Atc \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema='$SCHEMA' AND table_type='BASE TABLE'")
  if [ "$TABLES" = "0" ]; then
    docker compose exec -T postgres sh -c \
      "set -e; pg_dump -U $PG_USER --schema-only --no-owner --no-privileges -n $SCHEMA $MASTER_DB | psql -q -v ON_ERROR_STOP=1 -U $PG_USER -d $DB"
    echo "Schema copied"
  else
    echo "Schema already present ($TABLES tables)"
  fi

  # 2b. Таблицы public, используемые comment-service
  PUBLIC_OK=$(docker compose exec -T postgres psql -U "$PG_USER" -d "$DB" -Atc \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='comments'")
  if [ "$PUBLIC_OK" = "0" ]; then
    PG_DUMP_ARGS=""
    for t in "${PUBLIC_TABLES[@]}"; do PG_DUMP_ARGS="$PG_DUMP_ARGS -t public.$t"; done
    docker compose exec -T postgres sh -c \
      "set -e; pg_dump -U $PG_USER --schema-only --no-owner --no-privileges $PG_DUMP_ARGS $MASTER_DB | psql -q -v ON_ERROR_STOP=1 -U $PG_USER -d $DB"
    echo "Public comment tables copied"
  else
    echo "Public comment tables already present"
  fi

  # 3. Данные компании
  docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$DB" \
    -v cid="$CID" -v conn="host=$DBLINK_HOST port=$DBLINK_PORT dbname=$MASTER_DB user=$PG_USER password=$PG_PASS" <<'SQL'
CREATE EXTENSION IF NOT EXISTS dblink;

BEGIN;
SET LOCAL session_replication_role = replica;

-- Генерируем INSERT ... SELECT для каждой таблицы с фильтром по компании
SELECT format(
    'INSERT INTO %I.%I (%s) SELECT %s FROM dblink(%L, %L) AS s(%s) WHERE NOT EXISTS (SELECT 1 FROM %I.%I)',
    nsp, relname, cols, cols, :'conn',
    'SELECT ' || cols || ' FROM ' || nsp || '.' || quote_ident(relname) || ' ' || filt,
    coltypes, nsp, relname
)
FROM (
    SELECT
        nsp.nspname AS nsp,
        rel.relname,
        string_agg(quote_ident(att.attname), ', ' ORDER BY att.attnum) AS cols,
        string_agg(quote_ident(att.attname) || ' ' || format_type(att.atttypid, att.atttypmod), ', ' ORDER BY att.attnum) AS coltypes,
        CASE
            WHEN bool_or(att.attname = 'company_id')
                THEN 'WHERE company_id = ' || :cid::text
            WHEN rel.relname = 'companies'
                THEN 'WHERE id = ' || :cid::text
            WHEN rel.relname = 'employee_roles'
                THEN 'WHERE employee_id IN (SELECT id FROM svschema.employees WHERE company_id = ' || :cid::text || ')'
            ELSE ''
        END AS filt
    FROM pg_class rel
    JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum > 0 AND NOT att.attisdropped
    WHERE nsp.nspname IN ('svschema', 'public')
      AND rel.relkind = 'r'
      AND rel.relname NOT LIKE 'flyway%'
      AND rel.relname <> 'tenant_databases'
      AND (nsp.nspname = 'svschema' OR rel.relname IN
             ('comments','comment_replies','comment_images','reply_images','images','notifications'))
    GROUP BY nsp.nspname, rel.relname
) g
\gexec

COMMIT;

-- Сбрасываем последовательности после вставки явных id
SELECT format(
    'SELECT setval(%2$L, GREATEST((SELECT COALESCE(MAX(%3$I), 1) FROM %4$s), 1))',
    tbl, seq, col, tbl
)
FROM (
    SELECT quote_ident(c.table_schema) || '.' || quote_ident(c.table_name) AS tbl,
           pg_get_serial_sequence(c.table_schema || '.' || c.table_name, c.column_name) AS seq,
           c.column_name AS col
    FROM information_schema.columns c
    WHERE (c.table_schema = 'svschema'
        OR (c.table_schema = 'public' AND c.table_name IN
             ('comments','comment_replies','comment_images','reply_images','images','notifications')))
      AND c.column_default LIKE 'nextval%'
) s
WHERE seq IS NOT NULL
\gexec
SQL

  # 4. Регистрация в реестре тенантов
  docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$MASTER_DB" -c \
    "INSERT INTO $SCHEMA.tenant_databases (company_id, database_name) VALUES ($CID, '$DB')
     ON CONFLICT (company_id) DO UPDATE SET database_name = EXCLUDED.database_name"

  # 5. Проверка
  docker compose exec -T postgres psql -U "$PG_USER" -d "$DB" -Atc \
    "SELECT 'orders='||count(*) FROM $SCHEMA.orders
     UNION ALL SELECT 'clients='||count(*) FROM $SCHEMA.clients
     UNION ALL SELECT 'materials='||count(*) FROM $SCHEMA.materials
     UNION ALL SELECT 'employees='||count(*) FROM $SCHEMA.employees
     UNION ALL SELECT 'company_rows='||count(*) FROM $SCHEMA.companies
     UNION ALL SELECT 'comments='||count(*) FROM public.comments" | tr '\n' ' '
  echo
done

echo "=== tenant_databases registry ==="
docker compose exec -T postgres psql -U "$PG_USER" -d "$MASTER_DB" -c "SELECT * FROM $SCHEMA.tenant_databases ORDER BY company_id"
