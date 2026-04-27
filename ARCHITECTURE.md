# Архитектура системы PrintSV

## Обзор

Микросервисная система на стеке React + Spring Boot + Keycloak + PostgreSQL + Eureka с поддержкой OAuth2 авторизации и ролевой модели.

## Компоненты

### 1. Frontend (React 18 + TypeScript)

**Стек:**
- React 18, TypeScript
- Material-UI (MUI) для компонентов
- TanStack React Query для запросов к API
- TanStack Table v8 для таблиц
- React Router v6 для роутинга
- React Hook Form + Zod для валидации
- oidc-client-ts для OAuth2 авторизации

**Страницы:**
- `LoginPage` — страница входа
- `CallbackPage` — обработчик OAuth2 callback
- `OrdersList` — список заказов (editable grid)
- `OrderDetail` — детальная карточка заказа с вкладками:
  - Общая информация
  - Позиции (OrderItems)
  - Этапы производства (OrderStages)
  - Оплаты (Payments)
  - Комментарии
  - Файлы
- `Dashboard` — главная страница со статистикой

**Ключевые файлы:**
```
front/src/
├── context/AuthContext.jsx    # OIDC контекст
├── services/api.js            # Axios инстанс с JWT interceptor
├── pages/
│   ├── LoginPage.jsx
│   ├── CallbackPage.jsx
│   ├── OrdersList.jsx
│   ├── OrderDetail.jsx
│   └── Dashboard.jsx
└── App.jsx                    # Роутинг + ProtectedRoute
```

### 2. Backend микросервисы

#### Common модуль (`back/common`)

Общие сущности и DTO:

**Сущности (JPA):**
- `Order` — заказ
- `Client` — клиент
- `Employee` — сотрудник
- `OrderItem` — позиция заказа
- `OrderStage` — этап производства
- `Workshop` — цех
- `Material` — материал
- `OrderMaterial` — связь заказ-материал
- `Payment` — оплата
- `OrderComment` — комментарий
- `FileAttachment` — файл

**Enum'ы:**
- `OrderStatus` — WAITING, LAUNCHED, IN_PROGRESS, READY, ACCEPTED, CLOSED
- `ProductionStage` — NOT_STARTED, DESIGN, PRINTING, FINISHING, QUALITY_CONTROL, PACKAGING, SHIPPING
- `ClientType` — PRIVATE, COMPANY
- `ERole` — ADMIN, MANAGER, PRODUCTION, ACCOUNTANT, USER

**DTO:**
- `OrderDto`, `OrderCreateRequest`, `OrderItemDto`, `OrderStageDto`, `ClientDto`, `EmployeeDto`, `PaymentDto`, `CommentDto`

#### Order Service (`back/order-service`)

**Эндпоинты:**
```
GET    /api/v1/orders          — список заказов (фильтры, пагинация)
GET    /api/v1/orders/{id}     — детали заказа
POST   /api/v1/orders          — создать заказ
PUT    /api/v1/orders/{id}/status  — изменить статус
PUT    /api/v1/orders/{id}/stage   — изменить стадию
POST   /api/v1/orders/{id}/payments — добавить оплату
```

**Безопасность:**
- `ADMIN`, `MANAGER` — полный доступ
- `PRODUCTION` — обновление стадий
- `ACCOUNTANT` — управление оплатами

**Технологии:**
- Spring Boot 3.3, JPA, Liquibase
- Spring Security 6 + OAuth2 Resource Server (Keycloak JWT)
- SpringDoc OpenAPI 3 (Swagger UI)
- MapStruct (не используется в текущем коде, но настроен)

#### Client Service (`back/client-service`)

**Эндпоинты:**
```
GET    /api/v1/clients          — список клиентов (пагинация, поиск)
GET    /api/v1/clients/{id}     — детали клиента
POST   /api/v1/clients          — создать клиента
PUT    /api/v1/clients/{id}     — обновить клиента
DELETE /api/v1/clients/{id}     — удалить клиента
GET    /api/v1/clients/search?q=... — поиск
```

**Безопасность:**
- `ADMIN`, `MANAGER`, `ACCOUNTANT` — доступ

#### Employee Service (`back/employee-service`)

CRUD для сотрудников.

**Эндпоинты:**
```
GET    /api/v1/employees
GET    /api/v1/employees/{id}
POST   /api/v1/employees
PUT    /api/v1/employees/{id}
DELETE /api/v1/employees/{id}
```

**Безопасность:**
- `ADMIN`, `MANAGER` — чтение
- `ADMIN` — запись

#### Material Service (`back/material-service`)

CRUD для материалов (номенклатура).

**Эндпоинты:**
```
GET    /api/v1/materials
GET    /api/v1/materials/{id}
POST   /api/v1/materials
PUT    /api/v1/materials/{id}
DELETE /api/v1/materials/{id}
```

**Безопасность:**
- `ADMIN`, `MANAGER`, `PRODUCTION` — чтение
- `ADMIN` — запись

#### API Gateway (`back/api-gateway`)

**Технологии:** Spring Cloud Gateway (Reactive)

**Маршруты:**
- `/api/v1/orders/**` → `http://order-service:8081`
- `/api/v1/clients/**` → `http://client-service:8082`
- `/api/v1/employees/**` → `http://employee-service:8083`
- `/api/v1/materials/**` → `http://material-service:8084`

**Безопасность:** OAuth2 Resource Server (JWT валидация)

**CORS:** Разрешены запросы с `http://localhost:5174`

### 3. Инфраструктура

#### Eureka Discovery Server (`back/discovery-server`)

- Порт: 8762
- Dashboard: http://localhost:8762

#### Keycloak 26

**Конфигурация:**
- Realm: `print-sv`
- Порт: 8080
- Admin: `admin` / `admin`

**Clients:**
- `frontend` — SPA, Public client
  - Redirect URIs: `http://localhost:5174/*`
  - Web Origins: `http://localhost:5174`
- `order-service` — confidential client (service account)

**Роли (realm roles):**
- `ADMIN`
- `MANAGER`
- `PRODUCTION`
- `ACCOUNTANT`
- `USER`

**Тестовые пользователи (ключ:пароль):**
| Логин | Пароль | Роли |
|-------|--------|------|
| admin | admin | ADMIN |
| manager | manager | MANAGER |
| production | production | PRODUCTION |
| accountant | accountant | ACCOUNTANT |

#### PostgreSQL

- Порт: 5433
- Базы данных:
  - `svdb` — основные таблицы заказов, клиентов и т.д.
  - `keycloak_db` — данные Keycloak

**Подключение:**
```
Host: localhost
Port: 5433
Database: svdb
Username: postgres
Password: 12345
```

### 4. База данных — Liquibase миграции

Каждый сервис содержит миграции в `src/main/resources/db/changelog/db.changelog-master.yaml`

**Таблицы:**
- `orders` — заказы
- `order_items` — позиции
- `order_stages` — этапы производства
- `order_materials` — материалы в заказах
- `payments` — оплаты
- `order_comments` — комментарии
- `files` — файлы
- `clients` — клиенты
- `employees` — сотрудники
- `workshops` — цеха
- `materials` — номенклатура материалов

**Индексы:** Добавлены на часто используемые поля (status, dueDate, foreign keys)

## Схема взаимодействия

```
Frontend (React)
   ↓ HTTPS
API Gateway (Spring Cloud Gateway + OAuth2)
   ↓ ( маршрутизация + валидация JWT )
   ├─> Order Service (8081)
   ├─> Client Service (8082)
   ├─> Employee Service (8083)
   └─> Material Service (8084)

Keycloak (OAuth2 Provider,端口 8080)

Eureka (8762) — для service discovery

PostgreSQL (5433) — общая БД
```

## Запуск

### Вариант 1: Docker Compose (рекомендуется)

```bash
# Клонируйте репозиторий
cd sv

# Запустите все сервисы
docker-compose up -d

# Импортируйте Realm в Keycloak
# Откройте http://localhost:8080, войдите как admin/admin
# Перейдите в Realm Settings → Import и выберите keycloak/realm-export.json

# Откройте frontend
open http://localhost:5174
```

**Сервисы:**

| Сервис | URL | Порт |
|--------|-----|------|
| Keycloak Admin | http://localhost:8080 | 8080 |
| Eureka Dashboard | http://localhost:8762 | 8762 |
| Order Service API | http://localhost:8081 | 8081 |
| Order Service Swagger | http://localhost:8081/swagger-ui.html | |
| Client Service API | http://localhost:8082 | 8082 |
| Employee Service API | http://localhost:8083 | 8083 |
| Material Service API | http://localhost:8084 | 8084 |
| API Gateway | http://localhost:8085 | 8085 |
| Frontend | http://localhost:5174 | 5174 |

### Вариант 2: Локальная разработка

**Backend (Order Service):**
```bash
cd back/order-service
mvn spring-boot:run
```

**Frontend:**
```bash
cd front
npm install
npm run dev
```

**Keycloak:** Запустите через Docker:
```bash
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.1.4 start-dev
```

## OpenAPI спецификация

Файл: `docs/openapi-order-service.yaml`

Доступен Swagger UI:
- Order Service: http://localhost:8081/swagger-ui.html
- Client Service: http://localhost:8082/swagger-ui.html

## Дополнительно

### Настройка Keycloak

Для добавления новых пользователей:
1. Войдите в админ-консоль Keycloak
2. Выберите Realm `print-sv`
3. Вкладка "Users" → "Add user"
4. Укажите username, password и настройте роли во вкладке "Role mappings"

### Развертывание в production

1. Используйте внешнюю PostgreSQL (RDS) и Keycloak
2. Настройте HTTPS (TLS) для gateway и frontend
3. Настройте secure cookies в Keycloak
4. Используйте Docker secrets для паролей
5. Настройте мониторинг через Prometheus + Grafana (Actuator endpoints)

### Миграции

Для добавления новых полей/таблиц:
1. Создайте новый changeset в `db/changelog/db.changelog-master.yaml`
2. Увеличьте id changeset
3. Пересоберите и redeploy сервис

### Логирование

Логи пишутся в консоль. Для сбора используйте Docker logs или ELK stack.

## Контакты

Вопросы? Открывайте issue в репозитории.
