#!/bin/bash
# Смена публичного origin PrintSV (адрес, по которому фронтенд открывают браузеры).
#
# Нужен, когда сервер в локальной сети имеет динамический IP: весь стек уже ходит
# через nginx одного origin (/api/, /realms/ проксируются внутри sv-network), поэтому
# достаточно поменять три вещи: KEYCLOAK_PUBLIC_URL (от него зависит iss токенов и
# ссылки страницы входа Keycloak), CORS_ALLOWED_ORIGINS и allowlist redirectUris
# клиента frontend.
#
# Использование:
#   ./scripts/set-lan-origin.sh http://printsv.local:5174
#   ./scripts/set-lan-origin.sh http://192.168.1.50:5174 --skip-restart
#   ./scripts/set-lan-origin.sh http://192.168.1.50:5174 --skip-keycloak
#
# Флаги:
#   --skip-restart   только править .env (пересоздать контейнеры сам)
#   --skip-keycloak  не трогать redirectUris клиента frontend через Admin REST API
#
# Пароли из .env не выводятся; пересборка образов не требуется — SPA использует
# относительные URL (/api/..., /realms/...), а nginx принимает любой Host.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-$ROOT/.env}"
REALM="${KEYCLOAK_REALM:-print-sv}"
SPA_CLIENT="${KEYCLOAK_SPA_CLIENT_ID:-frontend}"
KC_ADMIN_BASE="${KEYCLOAK_ADMIN_URL:-http://127.0.0.1:8080}"

usage() {
  echo "Использование: $0 <origin> [--skip-restart] [--skip-keycloak]" >&2
  echo "Пример: $0 http://printsv.local:5174" >&2
  exit 1
}

ORIGIN=""
SKIP_RESTART="no"
SKIP_KEYCLOAK="no"
for arg in "$@"; do
  case "$arg" in
    --skip-restart) SKIP_RESTART="yes" ;;
    --skip-keycloak) SKIP_KEYCLOAK="yes" ;;
    -h|--help) usage ;;
    http://*|https://*) ORIGIN="$arg" ;;
    *) echo "Непонятный аргумент: $arg" >&2; usage ;;
  esac
done

[ -n "$ORIGIN" ] || usage
# Origin = схема + хост + порт, без пути и завершающего слэша
if ! [[ "$ORIGIN" =~ ^https?://[^[:space:]/:]+(:[0-9]{1,5})?$ ]]; then
  echo "Origin должен быть вида http://<хост-или-IP>:<порт> без пути: $ORIGIN" >&2
  exit 1
fi

AUTHORITY="${ORIGIN#*://}"
HOST_PART="${AUTHORITY%%:*}"
PORT="${AUTHORITY#*:}"
if [ "$PORT" = "$AUTHORITY" ]; then
  case "$ORIGIN" in https://*) PORT=443 ;; *) PORT=80 ;; esac
fi
if [ "$PORT" != "5174" ]; then
  echo "ВНИМАНИЕ: порт $PORT вместо 5174 — проверь, что nginx фронтенда опубликован на нём." >&2
fi

echo "=== Публичный origin: $ORIGIN (host=$HOST_PART port=$PORT) ==="

# ---------------------------------------------------------------- helpers ----
# Последнее значение ключа в .env
get_env() {
  local key="$1"
  [ -f "$ENV_FILE" ] || return 0
  grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | sed -n 's/^[^=]*=//p'
}

# Заменить строку KEY=... или дописать, если ключа нет (комментарии сохраняются)
upsert_env() {
  local key="$1" value="$2" tmp
  tmp="$(mktemp)"
  if [ -f "$ENV_FILE" ]; then
    awk -v k="$key" -v v="$value" '
      index($0, k "=") == 1 { print k "=" v; found = 1; next }
      { print }
      END { if (!found) print k "=" v }
    ' "$ENV_FILE" >"$tmp"
  else
    printf '%s=%s\n' "$key" "$value" >"$tmp"
  fi
  chmod 600 "$tmp"
  mv "$tmp" "$ENV_FILE"
}

urlencode() { printf '%s' "$1" | jq -sRr @uri; }

# Точная проверка наличия origin в CSV-списке (grep -F, чтобы * в масках не стал джокером)
csv_has() {
  local csv="$1" needle="$2"
  [ -n "$csv" ] || return 1
  printf '%s\n' "$csv" | tr ',' '\n' | grep -qxF "$needle"
}

# ------------------------------------------------------------ .env update ----
CORS_OLD="$(get_env CORS_ALLOWED_ORIGINS)"
CORS_NEW="$CORS_OLD"
for o in "$ORIGIN" "http://192.168.*:5174" "http://10.*:5174" "http://*.local:5174"; do
  if ! csv_has "$CORS_NEW" "$o"; then
    CORS_NEW="${CORS_NEW:+$CORS_NEW,}$o"
  fi
done

upsert_env "KEYCLOAK_PUBLIC_URL" "$ORIGIN"
upsert_env "CORS_ALLOWED_ORIGINS" "$CORS_NEW"
# SERVER_IP нужен только dev-прокси Vite (npm run dev); в контейнере не используется
upsert_env "SERVER_IP" "$HOST_PART"

echo "Обновлено в ${ENV_FILE}:"
echo "  KEYCLOAK_PUBLIC_URL=$ORIGIN"
echo "  CORS_ALLOWED_ORIGINS=$CORS_NEW"
echo "  SERVER_IP=$HOST_PART"

# ------------------------------------------------------------- re recreate ----
if [ "$SKIP_RESTART" = "no" ]; then
  echo "=== Пересоздание контейнеров (issuer и CORS задаются при старте) ==="
  docker compose up -d --force-recreate
  echo "=== Ожидание Keycloak и шлюза ==="
  ./scripts/wait-for-stack.sh "$ORIGIN" "http://127.0.0.1:8085" 180 ||
    echo "ВНИМАНИЕ: не всё поднялось за 180s — проверь docker compose ps и логи." >&2
else
  echo "Пропускаю пересоздание контейнеров (--skip-restart)."
fi

# ------------------------------------------------- Keycloak redirectUris ----
# realm-export.json импортируется при каждом старте Keycloak (--import-realm),
# поэтому клиент правим ПОСЛЕ пересоздания контейнеров.
if [ "$SKIP_KEYCLOAK" = "yes" ]; then
  echo "Пропускаю правку redirectUris (--skip-keycloak)."
elif ! command -v jq >/dev/null 2>&1; then
  echo "ВНИМАНИЕ: jq не найден — redirectUris клиента $SPA_CLIENT не обновлены." >&2
else
  KC_USER="$(get_env KC_BOOTSTRAP_ADMIN_USERNAME)"
  KC_PASS="$(get_env KC_BOOTSTRAP_ADMIN_PASSWORD)"
  KC_USER="${KC_USER:-admin}"
  if [ -z "$KC_PASS" ]; then
    echo "ВНИМАНИЕ: KC_BOOTSTRAP_ADMIN_PASSWORD пустой — redirectUris не обновлены." >&2
  else
    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "$tmp_dir"' EXIT
    body="$tmp_dir/token-form"
    client_json="$tmp_dir/client.json"
    printf 'grant_type=password&client_id=admin-cli&username=%s&password=%s' \
      "$(urlencode "$KC_USER")" "$(urlencode "$KC_PASS")" >"$body"
    chmod 600 "$body"

    if ! token="$(curl -fsS --max-time 20 -X POST \
      "$KC_ADMIN_BASE/realms/master/protocol/openid-connect/token" --data @"$body" | jq -r .access_token)" ||
      [ -z "$token" ] || [ "$token" = "null" ]; then
      echo "ВНИМАНИЕ: не получил токен администратора с $KC_ADMIN_BASE — обнови redirectUris вручную." >&2
    else
      client_id="$(curl -fsS --max-time 20 -H "Authorization: Bearer $token" \
        "$KC_ADMIN_BASE/admin/realms/$REALM/clients?clientId=$SPA_CLIENT" | jq -r '.[0].id // empty')"
      if [ -z "$client_id" ]; then
        echo "ВНИМАНИЕ: клиент $SPA_CLIENT в realm $REALM не найден." >&2
      else
        cur="$(curl -fsS --max-time 20 -H "Authorization: Bearer $token" \
          "$KC_ADMIN_BASE/admin/realms/$REALM/clients/$client_id")"
        # PUT принимает полное представление клиента — отдаём его же с дополненными списками
        printf '%s' "$cur" | jq --arg o "$ORIGIN" '
          .redirectUris = (((.redirectUris // []) + [$o + "/*", $o + "/callback"]) | unique)
          | .webOrigins = (((.webOrigins // []) + [$o]) | unique)
        ' >"$client_json"
        chmod 600 "$client_json"
        curl -fsS --max-time 20 -X PUT -H "Authorization: Bearer $token" \
          -H 'Content-Type: application/json' --data @"$client_json" \
          "$KC_ADMIN_BASE/admin/realms/$REALM/clients/$client_id" >/dev/null
        echo "redirectUris клиента $SPA_CLIENT дополнены origin $ORIGIN."
      fi
    fi
  fi
fi

echo
echo "=== Готово ==="
echo "Открой с другого компьютера сети: $ORIGIN"
echo "Проверь доступность порта с сервера: ss -lntp | grep 5174"
echo "Если firewall не пропускает сеть (пример для ufw):"
echo "  sudo ufw allow from 192.168.0.0/16 to any port 5174 proto tcp"
echo "Всем, кто уже вошёл, нужно перезайти: токены со старым iss больше не примутся."
