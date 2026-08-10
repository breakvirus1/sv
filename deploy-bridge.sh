#!/bin/bash
set -e

echo "=== Развертывание проекта SV Print через Bridge Network ==="

# Определение дистрибутива
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo "Не удалось определить ОС. Скрипт предназначен для RHEL/CentOS/Fedora."
    exit 1
fi

if [[ "$OS" != "rhel" && "$OS" != "centos" && "$OS" != "fedora" ]]; then
    echo "Эта ОС ($OS) не поддерживается. Используйте RHEL/CentOS/Fedora."
    exit 1
fi

# Проверка прав
if [ "$EUID" -ne 0 ]; then
    echo "Пожалуйста, запустите скрипт с sudo или от root."
    exit 1
fi

SUDO_USER=${SUDO_USER:-$USER}
USER_HOME=$(eval echo ~$SUDO_USER)
REPO_DIR="${REPO_DIR:-$USER_HOME/sv}"
REPO_URL="${REPO_URL:-https://q:1@github.com/breakvirus1/sv.git}"

echo "=== Шаг 1: Установка системных зависимостей ==="
yum install -y \
    curl \
    wget \
    gnupg2 \
    ca-certificates \
    git \
    yum-utils

echo "=== Шаг 2: Установка Docker ==="
if ! command -v docker &> /dev/null; then
    echo "Установка Docker..."
    yum install -y docker
    systemctl enable docker
    systemctl start docker
    usermod -aG docker $SUDO_USER
    echo "Docker установлен. Пользователь $SUDO_USER добавлен в группу docker."
else
    echo "Docker уже установлен: $(docker --version)"
fi

echo "=== Шаг 3: Установка Docker Compose ==="
if ! docker compose version &> /dev/null; then
    echo "Установка Docker Compose plugin..."
    yum install -y docker-compose-plugin
else
    echo "Docker Compose уже установлен: $(docker compose version)"
fi

echo "=== Шаг 4: Установка Java 21 ==="
if ! java -version 2>&1 | grep -q "21"; then
    echo "Установка OpenJDK 21..."
    yum install -y java-21-openjdk-devel
    echo "JAVA_HOME=/usr/lib/jvm/java-21-openjdk" >> /etc/environment
else
    echo "Java 21 уже установлена: $(java -version 2>&1 | head -n 1)"
fi

echo "=== Шаг 5: Установка Maven ==="
if ! command -v mvn &> /dev/null; then
    echo "Установка Maven..."
    yum install -y maven
else
    echo "Maven уже установлен: $(mvn -version | head -n 1)"
fi

echo "=== Шаг 6: Установка Node.js 20 и npm ==="
if ! command -v node &> /dev/null; then
    echo "Установка Node.js 20..."
    curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
    yum install -y nodejs
else
    echo "Node.js уже установлен: $(node --version)"
fi

if ! command -v npm &> /dev/null; then
    echo "Установка npm..."
    yum install -y npm
else
    echo "npm уже установлен: $(npm --version)"
fi

echo "=== Шаг 7: Установка PostgreSQL (опционально) ==="
if ! command -v psql &> /dev/null; then
    echo "Установка PostgreSQL клиента..."
    yum install -y postgresql-server postgresql-contrib
else
    echo "PostgreSQL клиент уже установлен: $(psql --version)"
fi

echo "=== Проверка статуса Docker ==="
systemctl status docker --no-pager || true

echo "=== Шаг 8: Создание Bridge Network ==="
NETWORK_NAME="sv-network"
if ! docker network ls | grep -q "$NETWORK_NAME"; then
    docker network create --driver bridge "$NETWORK_NAME"
    echo "Создана сеть $NETWORK_NAME"
else
    echo "Сеть $NETWORK_NAME уже существует"
fi

echo "=== Шаг 9: Клонирование/обновление репозитория ==="
if [ ! -d "$REPO_DIR" ]; then
    echo "Клонирование репозитория в $REPO_DIR..."
    mkdir -p "$(dirname "$REPO_DIR")"
    git clone "$REPO_URL" "$REPO_DIR"
    cd "$REPO_DIR"
else
    echo "Репозиторий уже существует в $REPO_DIR. Обновление..."
    cd "$REPO_DIR"
    git pull "$REPO_URL" main || git pull "$REPO_URL" master
fi

echo "=== Шаг 10: Сборка backend (Maven) ==="
cd "$REPO_DIR"
sudo -u $SUDO_USER mvn clean install -DskipTests || echo "Maven сборка завершена с предупреждениями"

echo "=== Шаг 11: Сборка frontend ==="
cd "$REPO_DIR/front"
sudo -u $SUDO_USER npm install
sudo -u $SUDO_USER npm run build
cd "$REPO_DIR"

echo "=== Шаг 12: Сборка Docker образов ==="
echo "Сборка postgres..."
docker build -t sv-postgres ./back/postgres || true

echo "Сборка discovery-server..."
docker build -t sv-discovery-server ./back/discovery-server

echo "Сборка api-gateway..."
docker build -t sv-api-gateway ./back/api-gateway

echo "Сборка order-service..."
docker build -t sv-order-service ./back/order-service

echo "Сборка client-service..."
docker build -t sv-client-service ./back/client-service

echo "Сборка employee-service..."
docker build -t sv-employee-service ./back/employee-service

echo "Сборка material-service..."
docker build -t sv-material-service ./back/material-service

echo "Сборка calculator-service..."
docker build -t sv-calculator-service ./back/calculator-service

echo "Сборка file-service..."
docker build -t sv-file-service ./back/file-service

echo "Сборка comment-service..."
docker build -t sv-comment-service ./back/comment-service

echo "Сборка generate-data-service..."
docker build -t sv-generate-data-service ./back/generate-data-service

echo "Сборка frontend..."
docker build -t sv-frontend ./front

echo "=== Шаг 13: Остановка старых контейнеров ==="
docker compose down || true
docker rm -f sv-postgres-1 sv-keycloak-1 sv-discovery-server-1 sv-order-service-1 sv-client-service-1 sv-employee-service-1 sv-material-service-1 sv-calculator-service-1 sv-file-service-1 sv-comment-service-1 sv-generate-data-service-1 sv-api-gateway-1 sv-frontend-1 2>/dev/null || true

echo "=== Шаг 14: Запуск PostgreSQL ==="
if [ ! "$(docker ps -q -f name=sv-postgres-1)" ]; then
    echo "Запуск PostgreSQL..."
    docker run -d \
        --name sv-postgres-1 \
        --network "$NETWORK_NAME" \
        -e POSTGRES_DB=svdb \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_PASSWORD=12345 \
        -v postgres_data:/var/lib/postgresql/data \
        -v "$REPO_DIR/init.sql:/docker-entrypoint-initdb.d/init.sql" \
        --health-cmd="pg_isready -U postgres" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=5 \
        postgres:15-alpine
else
    echo "PostgreSQL уже запущен"
fi

echo "=== Шаг 15: Запуск Keycloak ==="
if [ ! "$(docker ps -q -f name=sv-keycloak-1)" ]; then
    echo "Запуск Keycloak..."
    docker run -d \
        --name sv-keycloak-1 \
        --network "$NETWORK_NAME" \
        -p 8080:8080 \
        -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
        -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
        -e KC_DB=postgres \
        -e KC_DB_URL=jdbc:postgresql://sv-postgres-1:5432/svdb \
        -e KC_DB_USERNAME=postgres \
        -e KC_DB_PASSWORD=12345 \
        -e KC_HOSTNAME=localhost \
        -e KC_HOSTNAME_STRICT=false \
        -e KC_HTTP_ENABLED=true \
        -v "$REPO_DIR/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json" \
        --health-cmd="curl -f http://localhost:8080/health/ready" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=10 \
        quay.io/keycloak/keycloak:26.1.4 start-dev --import-realm
else
    echo "Keycloak уже запущен"
fi

echo "=== Шаг 16: Запуск Discovery Server ==="
if [ ! "$(docker ps -q -f name=sv-discovery-server-1)" ]; then
    echo "Запуск Discovery Server..."
    docker run -d \
        --name sv-discovery-server-1 \
        --network "$NETWORK_NAME" \
        -p 8761:8761 \
        -e SPRING_PROFILES_ACTIVE=docker \
        --health-cmd="curl -f http://localhost:8761/actuator/health" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=10 \
        sv-discovery-server
else
    echo "Discovery Server уже запущен"
fi

echo "=== Шаг 17: Запуск сервисов ==="
run_service() {
    local name=$1
    local image=$2
    local port=$3
    local env_vars=$4
    
    if [ "$(docker ps -q -f name=$name)" ]; then
        echo "$name уже запущен"
        return
    fi
    
    echo "Запуск $name..."
    docker run -d \
        --name "$name" \
        --network "$NETWORK_NAME" \
        -p "$port:$port" \
        $env_vars \
        --health-cmd="curl -f http://localhost:$port/actuator/health" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=10 \
        "$image"
}

run_service "sv-order-service-1" "sv-order-service" "8081" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-client-service-1" "sv-client-service" "8082" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-employee-service-1" "sv-employee-service" "8083" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-material-service-1" "sv-material-service" "8084" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-calculator-service-1" "sv-calculator-service" "8086" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-file-service-1" "sv-file-service" "8087" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-comment-service-1" "sv-comment-service" "8088" "-e SPRING_PROFILES_ACTIVE=docker"
run_service "sv-generate-data-service-1" "sv-generate-data-service" "8090" "-e SPRING_PROFILES_ACTIVE=docker"

echo "=== Шаг 18: Запуск API Gateway ==="
if [ ! "$(docker ps -q -f name=sv-api-gateway-1)" ]; then
    echo "Запуск API Gateway..."
    docker run -d \
        --name sv-api-gateway-1 \
        --network "$NETWORK_NAME" \
        -p 8085:8085 \
        -e SPRING_PROFILES_ACTIVE=docker \
        --health-cmd="curl -f http://localhost:8085/actuator/health" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=10 \
        sv-api-gateway
else
    echo "API Gateway уже запущен"
fi

echo "=== Шаг 19: Запуск Frontend ==="
if [ ! "$(docker ps -q -f name=sv-frontend-1)" ]; then
    echo "Запуск Frontend..."
    docker run -d \
        --name sv-frontend-1 \
        --network "$NETWORK_NAME" \
        -p 5174:5174 \
        sv-frontend
else
    echo "Frontend уже запущен"
fi

echo "=== Шаг 20: Очистка исходников ==="
cd "$REPO_DIR"
for service_dir in back/*/; do
    if [ -d "$service_dir/src" ]; then
        echo "Удаление src/ в $service_dir"
        rm -rf "$service_dir/src"
    fi
    if [ -d "$service_dir/target" ]; then
        echo "Удаление target/ в $service_dir"
        rm -rf "$service_dir/target"
    fi
done
if [ -d "front/src" ]; then
    echo "Удаление front/src/"
    rm -rf front/src
fi
if [ -d "front/node_modules" ]; then
    echo "Удаление front/node_modules/"
    rm -rf front/node_modules
fi
if [ -d "front/dist" ]; then
    echo "Удаление front/dist/"
    rm -rf front/dist
fi
echo "Очистка завершена."

echo "=== Шаг 21: Проверка статуса контейнеров ==="
docker ps --filter "name=sv-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "=== Развертывание завершено! ==="
echo ""
echo "Сервисы доступны по адресам:"
echo "  Frontend:        http://localhost:5174"
echo "  API Gateway:     http://localhost:8085"
echo "  Order Service:   http://localhost:8081"
echo "  Client Service:  http://localhost:8082"
echo "  Employee Service: http://localhost:8083"
echo "  Material Service: http://localhost:8084"
echo "  Calculator:      http://localhost:8086"
echo "  Comment Service: http://localhost:8088"
echo "  File Service:    http://localhost:8087"
echo "  Keycloak:        http://localhost:8080"
echo "  Discovery:       http://localhost:8761"
echo ""
echo "Для доступа из локальной сети используйте IP-адрес хоста вместо localhost."
echo "Пример: http://<host_ip>:5174"
echo ""
echo "Для просмотра логов: docker logs <container_name>"
echo "Для остановки: docker stop <container_name> && docker rm <container_name>"
echo ""
echo "Примечание: все контейнеры запущены в bridge-сети $NETWORK_NAME"
echo "и могут обращаться друг к другу по имени контейнера (например, sv-postgres-1:5432)."
echo ""
echo "Для переразвертывания:"
echo "  git pull $REPO_URL main"
echo "  mvn clean install -DskipTests"
echo "  cd front && npm install && npm run build && cd .."
echo "  docker compose down"
echo "  docker build ... (для каждого сервиса)"
echo "  docker run ... (для каждого контейнера)"
