# Развертывание Print SV на Windows

## Вариант 1: Podman (Рекомендуется)

### Требования
- Windows 10/11
- Права администратора
- [Podman Desktop](https://podman.io/) с поддержкой Compose

**WSL не требуется.** Podman Desktop на Windows работает через встроенный backend:
- **Рекомендуется**: WSL2 backend (автоматически настраивается Podman Desktop)
- **Альтернатива**: Hyper-V backend (если WSL недоступен или не нужен)

### Быстрый старт

1. Установите [Podman Desktop](https://podman.io/) и добавьте `podman` в PATH
2. Запустите от имени администратора:
   ```cmd
   scripts\windows\deploy-podman.bat
   ```

Скрипт автоматически:
- Запустит Podman сервис
- Соберёт все backend-сервисы через Maven
- Соберёт Docker-образы через Podman
- Запустит все сервисы

### Доступ к приложению

- **Frontend:** http://localhost:5174
- **API Gateway:** http://localhost:8085
- **Eureka Dashboard:** http://localhost:8761
- **Keycloak Admin:** http://localhost:8080 (admin / admin)
- **PostgreSQL:** localhost:5433

### Остановка всех сервисов

```cmd
scripts\windows\stop-podman.bat
```

### Просмотр логов

```cmd
podman compose -f podman-compose.yml -p sv logs -f <service-name>
```

## Вариант 2: Docker Desktop

### Требования
- Windows 10/11
- Права администратора
- Docker Desktop

### Быстрый старт

1. Установите [Docker Desktop](https://www.docker.com/products/docker-desktop/)
2. Запустите от имени администратора:
   ```cmd
   scripts\windows\deploy-native.bat
   ```

Скрипт автоматически установит все необходимые компоненты и запустит сервисы через Docker Compose.

### Доступ к приложению

- **Frontend:** http://localhost:5174
- **API Gateway:** http://localhost:8085
- **Eureka Dashboard:** http://localhost:8761
- **Keycloak Admin:** http://localhost:8080 (admin / admin)
- **PostgreSQL:** localhost:5433

### Остановка всех сервисов

```cmd
scripts\windows\stop-all.bat
```

## Сравнение вариантов

| Характеристика | Podman | Docker Desktop |
|---------------|--------|----------------|
| Лицензия | Open Source | Freemium |
| Ресурсы | Меньше | Больше |
| Демон | Нет (daemonless) | Требуется |
| WSL2 | Да | Да |
| Совместимость | Docker-compatible | Docker-native |

## Устранение неполадок

### Podman не запускается
Убедитесь что Podman Desktop установлен и добавлен в PATH:
```cmd
podman --version
```

Если используется WSL2 backend, убедитесь что WSL включён:
```cmd
wsl --install
```
Или переключитесь на Hyper-V backend в настройках Podman Desktop.

### Port already in use
Остановите все сервисы:
```cmd
scripts\windows\stop-podman.bat
```

### Cannot connect to PostgreSQL
Убедитесь что контейнер запущен:
```cmd
podman compose -f podman-compose.yml -p sv ps
```

### Frontend не открывается
Убедитесь что порт 5174 не занят:
```cmd
netstat -ano | findstr :5174
```
