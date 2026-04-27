# Print SV — Система управления производством

Микросервисная архитектура на базе React + Spring Boot + Keycloak + PostgreSQL + Eureka.

## Технологический стек

- **Frontend**: React 18, TypeScript, Material-UI, TanStack Query, TanStack Table, React Hook Form, Zod
- **Backend**: Spring Boot 3.3, Spring Security 6 (OAuth2 Resource Server), JPA/Hibernate, PostgreSQL, Eureka
- **Аутентификация**: Keycloak 26 (OAuth2 + OpenID Connect)
- **База данных**: PostgreSQL с Liquibase миграциями

## Структура проекта

```
back/
├── common/              # Общие сущности и DTO
├── discovery-server/    # Eureka Server
├── order-service/       # Сервис заказов
├── client-service/      # Сервис клиентов
└── api-gateway/         # API Gateway

front/                   # React frontend
keycloak/
└── realm-export.json    # Конфигурация Keycloak
docker-compose.yml       # Оркестрация контейнеров
init.sql               # Инициализация PostgreSQL
```

## Быстрый старт

### Requirements
- Docker и Docker Compose
- Node.js 18+ (для локальной разработки фронтенда)

### Запуск через Docker Compose

1. **Запустите все сервисы:**

```bash
docker-compose up -d
```

Сервисы будут доступны:
- Keycloak: http://localhost:8080
- PostgreSQL: localhost:5433 (базы: `svdb`, `keycloak_db`)
- Eureka: http://localhost:8762
- Order Service: http://localhost:8081
- Client Service: http://localhost:8082
- API Gateway: http://localhost:8085
- Frontend: http://localhost:5174

2. **Импортируйте Realm в Keycloak:**

Откройте http://localhost:8080 и войдите:
- Username: `admin`
- Password: `admin`

Перейдите в Realm Settings → Import. Выберите `keycloak/realm-export.json`.

3. **Откройте приложение:**

Frontend: http://localhost:5174

### Тестовые пользователи

| Логин  | Пароль  | Роли                           |
|--------|---------|--------------------------------|
| admin  | admin   | ADMIN                          |
| manager| manager | MANAGER                        |
| production | production | PRODUCTION                  |
| accountant | accountant | ACCOUNTANT                  |

## API Endpoints

### Order Service (http://localhost:8081)
- `GET /api/v1/orders` — список заказов (с фильтрами, пагинацией)
- `GET /api/v1/orders/{id}` — детали заказа
- `POST /api/v1/orders` — создать заказ
- `PUT /api/v1/orders/{id}/status` — изменить статус
- `POST /api/v1/orders/{id}/payments` — добавить оплату

### Client Service (http://localhost:8082)
- `GET /api/v1/clients` — список клиентов
- `POST /api/v1/clients` — создать клиента
- `GET /api/v1/clients/{id}` — детали клиента

### API Gateway (http://localhost:8085)
Проксирует запросы к микросервисам с проверкой JWT токенов.
- `POST /api/auth/token` — обмен authorization code на access token

## База данных

### Схема
Основные таблицы (Liquibase миграции):
- `orders` — заказы
- `order_items` — позиции заказа
- `order_stages` — этапы производства
- `order_materials` — материалы заказа
- `payments` — оплаты
- `order_comments` — комментарии
- `files` — файлы
- `clients` — клиенты
- `employees` — сотрудники
- `workshops` — цеха
- `materials` — номенклатура материалов

### Подключение
- Host: localhost
- Port: 5433
- Database: svdb
- Username: postgres
- Password: 12345

## Конфигурация Keycloak

### Realm
- `print-sv`

### Clients
- `frontend` — SPA приложение (public client)
- `order-service` — backend service (confidential, service account)

### Roles
- `ADMIN` — полный доступ
- `MANAGER` — управление заказами и клиентами
- `PRODUCTION` — управление этапами производства
- `ACCOUNTANT` — управление оплатами и аналитика
- `USER` — базовый доступ

## Локальная разработка

### Backend (Order Service)

```bash
cd back/order-service
mvn spring-boot:run
```

Сервис запустится на http://localhost:8081

### Frontend

```bash
cd front
npm install
npm run dev
```

Frontend запустится на http://localhost:5174

## Дополнительные возможности

- **Eureka Dashboard:** http://localhost:8762
- **Swagger UI (Order Service):** http://localhost:8081/swagger-ui.html
- **Swagger UI (Client Service):** http://localhost:8082/swagger-ui.html
- **Actuator (Order Service):** http://localhost:8081/actuator

## Безопасность

Все микросервисы (кроме gateway) работают как Resource Server и проверяют JWT токены от Keycloak.
- `issuer-uri`: http://localhost:8080/realms/print-sv
- Роли извлекаются из `realm_access.roles` и `resource_access.print-sv-client.roles`

## TODO / Дальнейшие шаги

- Реализовать остальные микросервисы (production-service, material-service, employee-service, finance-service, file-service)
- Интеграция с MinIO/S3 для хранения файлов
- Реалтайм уведомления через WebSocket
- Генерация PDF документов
- Планировщик задач для напоминаний о сроках
- Административная панель для управления шаблонами этапов
