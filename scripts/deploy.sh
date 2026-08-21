#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

info() {
  echo -e "\033[1;34m[INFO]\033[0m $1"
}

warn() {
  echo -e "\033[1;33m[WARN]\033[0m $1"
}

error() {
  echo -e "\033[1;31m[ERROR]\033[0m $1"
}

check_command() {
  if ! command -v "$1" &> /dev/null; then
    error "Команда '$1' не найдена. Установите её и попробуйте снова."
    exit 1
  fi
}

start_docker_desktop_windows() {
  if [[ "$(uname -s)" == *"MINGW"* ]] || [[ "$(uname -s)" == *"MSYS"* ]]; then
    if ! docker info &> /dev/null; then
      warn "Docker Desktop не запущен. Попытка запуска..."
      if command -v powershell.exe &> /dev/null; then
        powershell.exe -Command "Start-Process 'Docker Desktop' -ErrorAction SilentlyContinue" || true
      fi
      info "Ожидание запуска Docker Desktop (до 120 секунд)..."
      for i in {1..60}; do
        if docker info &> /dev/null; then
          info "Docker Desktop запущен."
          return 0
        fi
        sleep 2
      done
      error "Docker Desktop не запустился за отведённое время. Запустите его вручную."
      exit 1
    fi
  fi
}

check_docker() {
  if ! docker info &> /dev/null; then
    error "Docker не запущен или недоступен."
    exit 1
  fi
  info "Docker работает: $(docker version --format '{{.Server.Version}}')"
}

check_compose() {
  if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
  elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
  else
    error "Docker Compose не найден. Установите Docker Desktop с поддержкой Compose."
    exit 1
  fi
  info "Docker Compose: $($COMPOSE_CMD version --short 2>/dev/null || echo 'доступен')"
}

stop_existing() {
  info "Остановка существующих контейнеров..."
  $COMPOSE_CMD -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
}

build_images() {
  info "Сборка Docker-образов..."
  $COMPOSE_CMD -f "$COMPOSE_FILE" build --no-cache
}

start_services() {
  info "Запуск сервисов..."
  $COMPOSE_CMD -f "$COMPOSE_FILE" up -d --wait
}

show_status() {
  info "Статус контейнеров:"
  $COMPOSE_CMD -f "$COMPOSE_FILE" ps
}

show_urls() {
  echo ""
  info "Доступные сервисы:"
  echo "  Frontend:       http://localhost:5174"
  echo "  API Gateway:    http://localhost:8085"
  echo "  Discovery:      http://localhost:8761"
  echo "  Keycloak:       http://localhost:8080  (admin / admin)"
  echo "  Order Service:  http://localhost:8081"
  echo "  Client Service: http://localhost:8082"
  echo "  File Service:   http://localhost:8087"
  echo "  Comment Service:http://localhost:8088"
  echo "  Generate Data:  http://localhost:8090"
  echo "  PostgreSQL:     localhost:5433"
}

show_logs_hint() {
  echo ""
  info "Просмотр логов: $COMPOSE_CMD -f \"$COMPOSE_FILE\" logs -f <service-name>"
  info "Остановка:      $COMPOSE_CMD -f \"$COMPOSE_FILE\" down"
}

main() {
  info "Развёртывание проекта 'sv' в Docker (Windows 11)"
  echo ""

  check_command docker
  check_command git

  start_docker_desktop_windows
  check_docker
  check_compose

  cd "$PROJECT_ROOT"

  stop_existing
  build_images
  start_services
  show_status
  show_urls
  show_logs_hint

  echo ""
  info "Развёртывание завершено."
}

main "$@"
