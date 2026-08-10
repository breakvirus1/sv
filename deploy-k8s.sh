#!/bin/bash
set -e

echo "=== Развертывание проекта SV Print в Kubernetes ==="

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
K8S_DIR="$REPO_DIR/k8s"
NAMESPACE="sv-print"

echo "=== Шаг 1: Установка системных зависимостей ==="
yum install -y \
    curl \
    wget \
    gnupg2 \
    ca-certificates \
    git \
    yum-utils

echo "=== Шаг 2: Установка kubectl ==="
if ! command -v kubectl &> /dev/null; then
    echo "Установка kubectl..."
    curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.28/rpm/ | yum install -y kubectl
    echo "kubectl установлен: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
else
    echo "kubectl уже установлен: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
fi

echo "=== Шаг 3: Установка Docker ==="
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

echo "=== Шаг 7: Проверка/установка Kubernetes кластера ==="
if ! kubectl cluster-info &> /dev/null; then
    echo "Kubernetes кластер не доступен."
    echo "=== Установка Minikube ==="
    if ! command -v minikube &> /dev/null; then
        echo "Установка Minikube..."
        curl -fsSL https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64 -o /tmp/minikube-linux-amd64
        install /tmp/minikube-linux-amd64 /usr/local/bin/minikube
        rm /tmp/minikube-linux-amd64
    else
        echo "Minikube уже установлен: $(minikube version --short 2>/dev/null || minikube version)"
    fi
    
    echo "Запуск Minikube..."
    minikube start --driver=docker --cpus=4 --memory=8192
    
    echo "Настройка Docker окружения для Minikube..."
    eval $(minikube docker-env)
    echo "Docker окружение настроено для Minikube."
else
    echo "Kubernetes кластер доступен:"
    kubectl cluster-info --context $(kubectl config current-context) 2>/dev/null || kubectl cluster-info
fi

echo "=== Шаг 8: Создание namespace ==="
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

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

echo "=== Шаг 12: Создание Docker образов для Kubernetes ==="
# Если используем Minikube, настраиваем его Docker окружение
if command -v minikube &> /dev/null && minikube status &> /dev/null; then
    echo "Использование Docker окружения Minikube для сборки образов..."
    eval $(minikube docker-env)
fi

echo "Сборка postgres..."
docker build -t sv-postgres:k8s ./back/postgres || true

echo "Сборка discovery-server..."
docker build -t sv-discovery-server:k8s ./back/discovery-server

echo "Сборка api-gateway..."
docker build -t sv-api-gateway:k8s ./back/api-gateway

echo "Сборка order-service..."
docker build -t sv-order-service:k8s ./back/order-service

echo "Сборка client-service..."
docker build -t sv-client-service:k8s ./back/client-service

echo "Сборка employee-service..."
docker build -t sv-employee-service:k8s ./back/employee-service

echo "Сборка material-service..."
docker build -t sv-material-service:k8s ./back/material-service

echo "Сборка calculator-service..."
docker build -t sv-calculator-service:k8s ./back/calculator-service

echo "Сборка file-service..."
docker build -t sv-file-service:k8s ./back/file-service

echo "Сборка comment-service..."
docker build -t sv-comment-service:k8s ./back/comment-service

echo "Сборка generate-data-service..."
docker build -t sv-generate-data-service:k8s ./back/generate-data-service

echo "Сборка frontend..."
docker build -t sv-frontend:k8s ./front

# Отключаем Minikube Docker окружение, возвращаемся к хосту
if command -v minikube &> /dev/null && minikube status &> /dev/null; then
    echo "Возврат к хостовому Docker окружению..."
    eval $(minikube docker-env -u)
fi

echo "=== Шаг 13: Настройка Kubernetes манифестов ==="
mkdir -p "$K8S_DIR"

# Секрет с паролями
kubectl create secret generic sv-secrets \
    --from-literal=postgres-password=12345 \
    --from-literal=keycloak-admin-password=admin \
    --from-literal=keycloak-db-password=12345 \
    --namespace="$NAMESPACE" \
    --dry-run=client -o yaml | kubectl apply -f -

# ConfigMap с конфигурацией
kubectl create configmap sv-config \
    --from-literal=postgres-db=svdb \
    --from-literal=postgres-user=postgres \
    --from-literal=spring-profiles-active=docker \
    --from-literal=keycloak-hostname=localhost \
    --namespace="$NAMESPACE" \
    --dry-run=client -o yaml | kubectl apply -f -

# ConfigMap с init.sql
if [ -f "$REPO_DIR/init.sql" ]; then
    kubectl create configmap sv-init-sql \
        --from-file=init.sql="$REPO_DIR/init.sql" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
fi

# ConfigMap с realm-export.json
if [ -f "$REPO_DIR/keycloak/realm-export.json" ]; then
    kubectl create configmap sv-realm-export \
        --from-file=realm-export.json="$REPO_DIR/keycloak/realm-export.json" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
fi

echo "=== Шаг 14: Развертывание PostgreSQL ==="
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: $NAMESPACE
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: sv-config
              key: postgres-db
        - name: POSTGRES_USER
          valueFrom:
            configMapKeyRef:
              name: sv-config
              key: postgres-user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: sv-secrets
              key: postgres-password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: init-sql
          mountPath: /docker-entrypoint-initdb.d/init.sql
          subPath: init.sql
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
      - name: init-sql
        configMap:
          name: sv-init-sql
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: $NAMESPACE
spec:
  ports:
  - port: 5432
    targetPort: 5432
  selector:
    app: postgres
  type: ClusterIP
EOF

echo "=== Шаг 15: Развертывание Keycloak ==="
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: keycloak-pvc
  namespace: $NAMESPACE
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
      - name: keycloak
        image: quay.io/keycloak/keycloak:26.1.4
        args: ["start-dev", "--import-realm"]
        ports:
        - containerPort: 8080
        env:
        - name: KC_BOOTSTRAP_ADMIN_USERNAME
          value: "admin"
        - name: KC_BOOTSTRAP_ADMIN_PASSWORD
          valueFrom:
            secretKeyRef:
              name: sv-secrets
              key: keycloak-admin-password
        - name: KC_DB
          value: "postgres"
        - name: KC_DB_URL
          value: "jdbc:postgresql://postgres:5432/svdb"
        - name: KC_DB_USERNAME
          value: "postgres"
        - name: KC_DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: sv-secrets
              key: keycloak-db-password
        - name: KC_HOSTNAME
          valueFrom:
            configMapKeyRef:
              name: sv-config
              key: keycloak-hostname
        - name: KC_HOSTNAME_STRICT
          value: "false"
        - name: KC_HTTP_ENABLED
          value: "true"
        volumeMounts:
        - name: keycloak-storage
          mountPath: /opt/keycloak/data
        - name: realm-import
          mountPath: /opt/keycloak/data/import/realm-export.json
          subPath: realm-export.json
      volumes:
      - name: keycloak-storage
        persistentVolumeClaim:
          claimName: keycloak-pvc
      - name: realm-import
        configMap:
          name: sv-realm-export
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak
  namespace: $NAMESPACE
spec:
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: keycloak
  type: NodePort
EOF

echo "=== Шаг 16: Развертывание Discovery Server ==="
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: discovery-server
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: discovery-server
  template:
    metadata:
      labels:
        app: discovery-server
    spec:
      containers:
      - name: discovery-server
        image: sv-discovery-server:k8s
        ports:
        - containerPort: 8761
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: sv-config
              key: spring-profiles-active
---
apiVersion: v1
kind: Service
metadata:
  name: discovery-server
  namespace: $NAMESPACE
spec:
  ports:
  - port: 8761
    targetPort: 8761
  selector:
    app: discovery-server
  type: NodePort
EOF

echo "=== Шаг 17: Развертывание сервисов ==="
services=("order-service:8081" "client-service:8082" "employee-service:8083" "material-service:8084" "calculator-service:8086" "file-service:8087" "comment-service:8088" "generate-data-service:8090")

for service_port in "${services[@]}"; do
    service="${service_port%%:*}"
    port="${service_port##*:}"
    
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $service
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: $service
  template:
    metadata:
      labels:
        app: $service
    spec:
      containers:
      - name: $service
        image: sv-$service:k8s
        ports:
        - containerPort: $port
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: sv-config
              key: spring-profiles-active
---
apiVersion: v1
kind: Service
metadata:
  name: $service
  namespace: $NAMESPACE
spec:
  ports:
  - port: $port
    targetPort: $port
  selector:
    app: $service
  type: NodePort
EOF
done

echo "=== Шаг 18: Развертывание API Gateway ==="
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: sv-api-gateway:k8s
        ports:
        - containerPort: 8085
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: sv-config
              key: spring-profiles-active
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: $NAMESPACE
spec:
  ports:
  - port: 8085
    targetPort: 8085
  selector:
    app: api-gateway
  type: NodePort
EOF

echo "=== Шаг 19: Развертывание Frontend ==="
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: sv-frontend:k8s
        ports:
        - containerPort: 5174
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: $NAMESPACE
spec:
  ports:
  - port: 5174
    targetPort: 5174
  selector:
    app: frontend
  type: NodePort
EOF

echo "=== Шаг 20: Создание Ingress (опционально) ==="
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: sv-ingress
  namespace: $NAMESPACE
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: sv.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 5174
      - path: /api/
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8085
      - path: /keycloak/
        pathType: Prefix
        backend:
          service:
            name: keycloak
            port:
              number: 8080
EOF

echo "=== Шаг 21: Ожидание запуска подов ==="
echo "Ожидание запуска PostgreSQL..."
kubectl wait --for=condition=ready pod -l app=postgres -n "$NAMESPACE" --timeout=120s || true

echo "Ожидание запуска Keycloak..."
kubectl wait --for=condition=ready pod -l app=keycloak -n "$NAMESPACE" --timeout=120s || true

echo "Ожидание запуска Discovery Server..."
kubectl wait --for=condition=ready pod -l app=discovery-server -n "$NAMESPACE" --timeout=120s || true

echo "Ожидание запуска сервисов..."
kubectl wait --for=condition=ready pod -l app=order-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=client-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=employee-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=material-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=calculator-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=file-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=comment-service -n "$NAMESPACE" --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=generate-data-service -n "$NAMESPACE" --timeout=120s || true

echo "Ожидание запуска API Gateway..."
kubectl wait --for=condition=ready pod -l app=api-gateway -n "$NAMESPACE" --timeout=120s || true

echo "Ожидание запуска Frontend..."
kubectl wait --for=condition=ready pod -l app=frontend -n "$NAMESPACE" --timeout=120s || true

echo "=== Шаг 22: Получение NodePort адресов ==="
echo "NodePort адреса для доступа из локальной сети:"
kubectl get services -n "$NAMESPACE" -o wide

echo ""
echo "=== Развертывание в Kubernetes завершено! ==="
echo ""
echo "Namespace: $NAMESPACE"
echo ""
echo "Сервисы доступны:"
echo ""

# Получаем NodePort для каждого сервиса
get_nodeport() {
    local service=$1
    local port=$(kubectl get service "$service" -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "pending")
    echo "  $service: http://<node_ip>:$port"
}

get_nodeport "frontend"
get_nodeport "api-gateway"
get_nodeport "order-service"
get_nodeport "client-service"
get_nodeport "employee-service"
get_nodeport "material-service"
get_nodeport "calculator-service"
get_nodeport "comment-service"
get_nodeport "file-service"
get_nodeport "keycloak"
get_nodeport "discovery-server"

echo ""
echo "Для получения NodePort адресов выполните:"
echo "  kubectl get services -n $NAMESPACE"
echo ""
echo "Для просмотра логов:"
echo "  kubectl logs -n $NAMESPACE <pod-name>"
echo ""
echo "Для удаления всех ресурсов:"
echo "  kubectl delete namespace $NAMESPACE"
echo ""
echo "Примечание: PostgreSQL и Keycloak используют PersistentVolumeClaim для хранения данных."
echo "Все сервисы общаются через Kubernetes Service Discovery по имени сервиса (например, postgres:5432)."
echo ""
echo "Для доступа через Ingress (если настроен):"
echo "  Добавьте в /etc/hosts: <node_ip> sv.local"
echo "  Frontend: http://sv.local"
echo "  API Gateway: http://sv.local/api/"
echo "  Keycloak: http://sv.local/keycloak/"
echo ""
echo "Для Minikube:"
echo "  minikube service list -n $NAMESPACE"
echo "  minikube service <service-name> -n $NAMESPACE --url"
