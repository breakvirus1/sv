# Print SV — Система управления производством

Микросервисная система автоматизации рекламно-печатного производства (широкоформатная печать): заказы, этапы производства по цехам, калькуляция операций, материалы, клиенты, оплаты, сотрудники, статистика. Использует docker-compose для оркестрации: React frontend + Spring Boot microservices + Keycloak + PostgreSQL + Eureka.

## Технологический стек

- **Frontend**: React 18 + Vite (JavaScript/JSX), Material-UI, TanStack Query, TanStack Table, React Hook Form, Zod, zustand, oidc-client-ts; тесты — Vitest (unit) + Playwright (e2e)
- **Backend**: Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.3 (Eureka, Gateway), Spring Security OAuth2 Resource Server, Spring Data JPA + Specification, MapStruct, Lombok, springdoc-openapi (Swagger UI)
- **Миграции БД**: Flyway
- **Аутентификация**: Keycloak 26 (OAuth2 + OpenID Connect), мультитенантность через атрибут пользователя `company_id` (передается в токене)
- **База данных**: PostgreSQL 15, общая БД `svdb`, схема `svschema`

## Структура проекта

```
back/
├── common/                 # Общие сущности и DTO
├── discovery-server/       # Eureka Server
├── api-gateway/            # Spring Cloud Gateway (маршрутизация, обмен code -> token)
├── order-service/          # Заказы, позиции, этапы, оплаты, сметы
├── client-service/         # Клиенты и компании (tenant-корень, сиды дефолтов)
├── employee-service/       # Сотрудники (синхронизация с Keycloak Admin API)
├── material-service/       # Справочник материалов, material-operations
├── calculator-service/     # Калькуляция операций (раскрой, печать, ламинация, люверсы...)
├── file-service/           # Файлы
├── comment-service/        # Комментарии, ответы, уведомления
├── statistic-service/      # Статистика (sync из order-service)
└── generate-data-service/  # Генератор тестовых данных (dev-профиль)

front/                      # React frontend
scripts/                    # deploy.sh, wait-for-stack.sh, rebuild-*.ps1
keycloak/
└── realm-export.json       # Realm print-sv (импортируется автоматически)
docs/
└── openapi-order-service.yaml
docker-compose.yml          # Оркестрация всех сервисов
.env.example                # Шаблон переменных окружения (скопируйте в .env)
```

## Быстрый старт

### Требования
- Docker и Docker Compose
- Node.js 20+ (для локальной разработки фронтенда) / JDK 17 + Maven (для бэкенда)

### Запуск через Docker Compose

1. **Создайте `.env`** (см. `.env.example`):

```bash
cp .env.example .env
# укажите SERVER_IP, KEYCLOAK_PUBLIC_URL и надёжные пароли
```

2. **Запустите все сервисы:**

```bash
docker compose up -d --build
# генератор тестовых данных (только для разработки):
docker compose --profile dev up -d generate-data-service
```

Realm `print-sv` импортируется автоматически (`--import-realm`). По умолчанию
используется `keycloak/realm-export.json` — production-safe realm **без
пользователей и клиентских секретов**. Первого администратора создайте вручную
в Keycloak Admin Console, задайте ему `company_id` и создайте confidential-клиент
`order-service` (секрет пропишите в `KEYCLOAK_ADMIN_CLIENT_SECRET`).

Для локальной разработки/e2e можно импортировать dev-realm с демо-пользователями:

```bash
KEYCLOAK_REALM_IMPORT=./keycloak/realm-export.dev.json docker compose up -d --build
```

Снаружи доступен только фронтенд (через него проксируются API и Keycloak).
Отладочные порты привязаны к loopback и недоступны из локальной сети:
- Frontend: http://localhost:5174 (`/realms/print-sv`, `/api/...`)
- API Gateway: http://127.0.0.1:8085 (только loopback)
- Keycloak: http://127.0.0.1:8080 (только loopback, для локальной разработки)
- Внутренние сервисы (PostgreSQL, Eureka, order/client/file/comment/statistic)
  наружу не публикуются.

### Тестовые пользователи (только dev-realm)

| Логин  | Пароль  | Роли                                   |
|--------|---------|----------------------------------------|
| admin  | admin   | ADMIN                                  |
| manager| manager | MANAGER                                |
| production | production | PRODUCTION                       |
| accountant | accountant | ACCOUNTANT                       |

Эти учётки находятся только в `keycloak/realm-export.dev.json` и не должны
использоваться в продакшене. У всех демо-пользователей задан `company_id=1`;
этот атрибут определяет компанию-tenant (изоляция данных). Пользователь без
`company_id` получает доступ только при наличии роли `GOD`.
При создании новой компании автоматически добавляются дефолтные материалы и операции (CompanyDefaultsSeeder).

Вход выполняется по OAuth2 Authorization Code + PKCE; password grant отключён.

## API Endpoints

Все запросы идут через nginx фронтенда (`/api/...`). API Gateway опубликован
только на loopback (`http://127.0.0.1:8085`) для отладки; внутренние порты
сервисов наружу не публикуются.

### Order Service (через gateway)
- `GET /api/v1/orders` — список заказов (с фильтрами, пагинацией)
- `GET /api/v1/orders/{id}` — детали заказа
- `POST /api/v1/orders` — создать заказ
- `PUT /api/v1/orders/{id}/status` — изменить статус
- `POST /api/v1/orders/{id}/payments` — добавить оплату

### Client Service (через gateway)
- `GET /api/v1/clients` — список клиентов
- `POST /api/v1/clients` — создать клиента
- `GET /api/v1/clients/{id}` — детали клиента

### API Gateway (http://localhost:8085)
Проксирует запросы к микросервисам с проверкой JWT токенов.
- `POST /api/v1/auth/token` — обмен authorization code + PKCE на токены
- `POST /api/v1/auth/refresh` — обновление токенов

## База данных

### Схема
Основные таблицы (Flyway-миграции, схема `svschema`):
- `orders` — заказы
- `order_items` — позиции заказа
- `order_stages` — этапы производства
- `order_materials` — материалы заказа
- `payments` — оплаты
- `order_comments` — комментарии
- `files` — файлы
- `clients` — клиенты
- `companies` — компании (tenants)
- `employees` — сотрудники
- `workshops` — цеха
- `materials` — номенклатура материалов

Подключение (локальная БД из docker-compose) настраивается переменными окружения в `.env` (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`).

## Конфигурация Keycloak

### Realm
- `print-sv`

### Clients
- `frontend` — SPA приложение (public client)
- `order-service` — backend service (confidential, service account)

### Роли
- `ADMIN` — полный доступ
- `MANAGER` — управление заказами и клиентами
- `PRODUCTION` — управление этапами производства
- `ACCOUNTANT` — управление оплатами и аналитика
- `USER` — базовый доступ

Все сервисы фильтруют данные по `company_id` из JWT (мультитенантность).

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

Тесты и сборка описаны в CI (.github/workflows/ci-cd.yml): backend `mvn test`, frontend `npm test` (Vitest), e2e — Playwright.

## Дополнительные возможности

- **Eureka Dashboard:** доступен только внутри docker-сети (`http://discovery-server:8761`)
- **Swagger UI:** через gateway, например `http://localhost:8085/swagger-ui.html`
- **Actuator:** только внутри docker-сети (`http://order-service:8081/actuator`)

## Безопасность

Все микросервисы (кроме gateway) работают как Resource Server и проверяют JWT токены от Keycloak.
- `issuer-uri`: ${KEYCLOAK_PUBLIC_URL}/realms/print-sv (по умолчанию http://localhost:5174/realms/print-sv)
- Роли извлекаются из `realm_access.roles` и `resource_access.print-sv-client.roles`

Не храните секреты в репозитории — используйте `.env` (в `.gitignore`). Сессии/токены не сохраняйте в файлах вроде `cookies.txt`.

## TODO / Дальнейшие шаги

- [ ] Интеграция с MinIO/S3 для хранения файлов (сейчас локальный volume `./uploads`)
- [ ] Реалтайм уведомления через WebSocket
- [ ] Генерация PDF документов
- [ ] Планировщик задач для напоминаний о сроках
- [ ] Административная панель для управления шаблонами этапов


## Справочник: заполнение операций для производства изделий из баннеров

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
Калькуляция: Периметр = 2 × (Ш + В)
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
