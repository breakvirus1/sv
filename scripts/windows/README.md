# Развертывание Print SV на Windows без Docker/Podman

## Требования

- Windows 10/11
- Права администратора
- Интернет-соединение для автоматической установки зависимостей

## Быстрый старт

### Автоматическая установка

Скрипт автоматически установит все необходимые компоненты:
- Java 17 (Temurin JDK)
- Apache Maven 3.8+
- Node.js 18+ LTS
- PostgreSQL 15+
- Keycloak 26+

Запустите от имени администратора:
```cmd
scripts\windows\deploy-native.bat
```

Скрипт проверит наличие каждого компонента и установит его при необходимости через:
1. `winget` (встроенный менеджер пакетов Windows 10/11)
2. `choco` (Chocolatey, если установлен)

После установки всех компонентов скрипт автоматически:
- Создаст базу данных `svdb` в PostgreSQL
- Запустит Keycloak
- Соберёт все backend-сервисы через Maven
- Запустит все сервисы
- Запустит фронтенд

### Ручная установка (если автоматическая не работает)

Если автоматическая установка не работает, установите компоненты вручную:

| Компонент | Версия | Ссылка |
|-----------|--------|--------|
| Java JDK | 21 | https://adoptium.net/ |
| Apache Maven | 3.8+ | https://maven.apache.org/ |
| Node.js | 18+ | https://nodejs.org/ |
| PostgreSQL | 15+ | https://www.postgresql.org/download/windows/ |
| Keycloak | 26+ | https://www.keycloak.org/downloads |

После ручной установки запустите:
```cmd
scripts\windows\deploy-native.bat
```

Скрипт автоматически:
- Установит все недостающие зависимости (Java 21, Maven, Node.js, PostgreSQL, Keycloak)
- Создаст базу данных `svdb` в PostgreSQL
- Запустит Keycloak
- Соберёт все backend-сервисы через Maven
- Запустит все backend-сервисы
- Установит зависимости фронтенда через npm
- Запустит фронтенд на порту 5174

## Доступ к приложению

- **Frontend:** http://localhost:5174 или http://192.168.88.118:5174
- **Keycloak Admin:** http://localhost:8080 (admin / admin)
- **API Gateway:** http://localhost:8085
- **Eureka Dashboard:** http://localhost:8761

## Управление сервисами

### Остановка всех сервисов
```cmd
scripts\windows\stop-all.bat
```

### Просмотр логов
Логи выводятся в скрытые окна. Для просмотра логов конкретного сервиса можно запустить его вручную:

```cmd
:: Order Service
cd back\order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

:: Frontend
cd front
npm run dev -- --host
```

### Перезапуск конкретного сервиса
1. Остановите все сервисы: `scripts\windows\stop-all.bat`
2. Запустите нужный сервис вручную:
```cmd
cd back\order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Конфигурация

### Переменные окружения

Скрипт устанавливает следующие переменные окружения:
- `EUREKA_URL=http://localhost:8761/eureka/`
- `DB_HOST=localhost`
- `KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/print-sv`

### Настройка front/.env

Для локального запуска фронтенда через `npm run dev` создайте файл `front\.env`:

```env
VITE_API_URL=http://192.168.88.118:8085
VITE_KEYCLOAK_ISSUER=http://192.168.88.118:8080/realms/print-sv
VITE_KEYCLOAK_CLIENT_ID=frontend
```

Или используйте шаблон:
```cmd
copy front\.env.example front\.env
```

### Настройка PostgreSQL

По умолчанию используется:
- Host: localhost
- Port: 5432
- Database: svdb
- Username: postgres
- Password: 12345

Если у вас другие параметры, измените их в скрипте `deploy-native.ps1` или в конфигурации сервисов.

### Импорт Keycloak Realm

После запуска Keycloak:
1. Откройте http://localhost:8080
2. Войдите как admin / admin
3. Перейдите в **Realm Settings** → **Import**
4. Выберите файл `keycloak\realm-export.json`
5. Нажмите **Import**

## Порты

Следующие порты должны быть свободны:

| Порт | Сервис |
|------|--------|
| 5432 | PostgreSQL |
| 8080 | Keycloak |
| 8761 | Eureka Server |
| 8081 | Order Service |
| 8082 | Client Service |
| 8083 | Employee Service |
| 8084 | Material Service |
| 8086 | Calculator Service |
| 8087 | File Service |
| 8088 | Comment Service |
| 8090 | Generate Data Service |
| 8085 | API Gateway |
| 5174 | Frontend |

## Устранение неполадок

### Ошибка: "Port already in use"
Остановите все сервисы и попробуйте снова:
```cmd
scripts\windows\stop-all.bat
```

### Ошибка: "Cannot connect to PostgreSQL"
Убедитесь что PostgreSQL запущен:
```cmd
net start postgresql
```

### Ошибка: "Keycloak not found"
Убедитесь что Keycloak установлен в `C:\keycloak` или измените путь в скрипте.

### Ошибка: "Maven build failed"
Убедитесь что Java 21 и Maven 3.8+ установлены и доступны в PATH:
```cmd
java -version
mvn -version
```

### Ошибка при установке зависимостей
Скрипт пытается установить недостающие компоненты через `winget` или `choco`.
Если автоматическая установка не работает:
1. Установите компоненты вручную
2. Перезапустите скрипт

### Keycloak установлен, но не запускается
Убедитесь что порт 8080 не занят:
```cmd
netstat -ano | findstr :8080
```
Если порт занят, остановите процесс или измените порт Keycloak.

### Frontend не открывается
Убедитесь что порт 5174 не занят:
```cmd
netstat -ano | findstr :5174
```

## Альтернативный запуск через Docker/Podman

Если нативный запуск не работает, используйте Docker/Podman:

```cmd
:: Если установлен Podman
scripts\windows\start-with-podman.bat

:: Или через Docker Desktop
docker compose up -d
```

## Поддержка

При возникновении проблем:
1. Проверьте логи консоли
2. Убедитесь что все порты свободны
3. Проверьте что все сервисы запущены
