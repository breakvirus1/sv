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


Справочник по заполнению операций для производства изделий из баннеров
1. Раскрой баннера (пошив)
Шаблон: "Раскрой баннера" с requiresDimensions: true, unit: 'м²'

Поля формы:

Ширина (мм) — ширина будущего изделия
Высота (мм) — высота изделия
Припуск на подворот — обычно 50-100 мм с каждой стороны
Припуск ширина: 50 (припуск на левый и правый край)
Припуск высота: 50 (припуск на верх и низ)
Кратный коэффициенты: 2 (если нужен подворот с 2-х сторон) или 1 (для простого обреза)
Что считается:

Расчет идет по итоговой площади с учетом вышеуказанных параметров.
Калькуляция:

Эффективная ширина = ширина + (припуск ширина × 2)
Эффективная высота = высота + (припуск высота × 2)
Кол-во м² = (Ш × В) / 1 000 000 × коэффициент
2. Печать баннера
Шаблон: "Печать баннера" с requiresDimensions: true, unit: 'м²'

Поля формы:

Ширина (мм)
Высота (мм)
Припуск обычно не нужен (работаем с чистыми размерами)
Параметры:
materialType (выбор: баннер 1000gr, баннер 380gr, сетка и т.д.)
resolution (разрешение печати: 720dpi, 1440dpi)
3. Ламинирование/оклейка
Шаблон: "Ламинирование" с requiresDimensions: true, unit: 'м²'

Поля:

Ширина/Высота — того же размера что и у печати
Припуск — только если ламинируют с нахлестом (ламинирующая пленка больше печати)
Припуск ширина: 20 мм с каждой стороны
Припуск высота: 20 мм
Коэффициент: 1 (одностороннее ламинирование)
4. Установка люверсов
Шаблон: "Установка люверсов" с requiresDimensions: false, unit: 'шт.'

Поля:

Количество люверсов — указывается вручную в параметрах
Параметры:
diameter (диаметр люверса: 8мм, 12мм, 16мм)
edgeDistance (отступ от края: 30-50 мм)
Примечание: Отсутствуют размеры изделия, количество задается явно.

5. Сварка/пошив каркаса
Шаблон: "Сварка каркаса" с requiresDimensions: true, unit: 'пог.м'

Поля:

Ширина (мм)
Высота (мм)
Припуск — не требуется (работа с готовыми профилями)
Калькуляция: Периметр = 2 × (Ш + В), Hansen формула
6. Монтаж конструкции
Шаблон: "Монтаж баннера" с requiresDimensions: false, unit: 'шт.'

Поля:

Количество — задается вручную (1 шт. на 1 конструкцию)
Параметры:
mountType (тип монтажа: настенный, кронштейн, creep)
height (высота установки — влияет на сложность)
Общие рекомендации:
1. Для всех операций с раскроем:

Используйте припуск 50-100 мм на каждую сторону
Если нужно подворачивать с двух сторон — ставьте коэффициент 2
Если нужен припуск только сверху/снизу — вписывайте значения с учетом (например, 100 сверху, 0 снизу)
2. Для печати:

Заводите размеры в миллиметрах
Площадь считается в м² с точностью до 4 знаков
Не добавляйте припуск, если клиент не просил
3. Для дополнительных материалов (люверсы, крепления):

Добавляйте через "Дополнительные материалы"
Количество = количество точек крепления (обычно 4 шт. на стандартный баннер)
4. Налоги:

Припуск учитывается в общей стоимости материала
В operations параметры добавляйте процент отходов (например, wasteCoefficient: 1.1 для 10% отходов)
Нужно адаптировать эти рекомендации под ваши конкретные шаблоны операций в базе данных?