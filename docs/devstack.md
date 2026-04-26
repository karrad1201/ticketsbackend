# DevStack — локальный prod-like стек

`docker-compose.devstack.yml` разворачивает production-подобное окружение:

```
[эмулятор/браузер]
        ↓ :80
    [ nginx ]          ← единственный открытый порт
    /       \
 [app1]   [app2]       ← Spring Boot, profile=devstack
    \       /
  [postgres] [redis]   ← internal only
```

---

## Быстрый старт

```bash
# 1. Собрать образ из исходников
make devstack-build

# 2. Запустить стек
make devstack-up

# 3. Проверить здоровье
curl http://localhost/actuator/health
```

API доступен по адресу `http://localhost/api/...`

Swagger UI: `http://localhost/swagger-ui/index.html`

---

## Предзасеянные аккаунты

Все аккаунты готовы сразу после старта. Можно использовать готовые токены **или** логиниться через SMS (код всегда `123456`).

| Роль          | Телефон       | Bearer-токен             |
|---------------|---------------|--------------------------|
| ADMIN         | +79991000001  | `devstack-admin-token`   |
| Org OWNER     | +79991000002  | `devstack-owner-token`   |
| Org STAFF     | +79991000003  | `devstack-staff-token`   |
| Обычный user  | +79991000004  | `devstack-user-token`    |

Пример запроса:
```bash
curl -H "Authorization: Bearer devstack-owner-token" http://localhost/api/organizations/my
```

### Организация
- Название: **DevStack Fest**
- Код: `devstack-org`

### Площадка
- Название: **Арена DevStack**, Москва, ул. Тестовая, 1

### Мероприятия

| Название                  | Когда       | Билеты                              |
|---------------------------|-------------|-------------------------------------|
| DevStack Open Air         | +7 дней     | Стандарт: 1 000 руб × 500 шт        |
| DevStack Conference       | +14 дней    | Стандарт: 2 500 руб × 200 шт; VIP: 7 500 руб × 50 шт |
| DevStack After Party      | +30 дней    | Стандарт: 500 руб × 100 шт          |

---

## Makefile targets

| Команда                  | Описание                                         |
|--------------------------|--------------------------------------------------|
| `make devstack-build`    | Сборка JAR + Docker-образ                        |
| `make devstack-up`       | Запустить стек в фоне                            |
| `make devstack-down`     | Остановить контейнеры (volumes сохраняются)      |
| `make devstack-restart`  | Перезапустить только app1 и app2                 |
| `make devstack-logs`     | Следить за логами всех сервисов                  |
| `make devstack-push`     | Запушить образ в Docker Hub                      |
| `make devstack-psql`     | Открыть psql в postgres-контейнере               |
| `make devstack-redis-flush` | Сбросить Redis-кэш                           |
| `make devstack-sms`      | Показать SMS-коды из логов (номер телефона)      |

---

## Переменные окружения

| Переменная      | Default                              | Описание               |
|-----------------|--------------------------------------|------------------------|
| `IMAGE`         | `karrad1201/bilets-backend`          | Имя образа             |
| `TAG`           | `latest`                             | Тег образа             |
| `BILETS_IMAGE`  | `karrad1201/bilets-backend:latest`   | Полный образ в compose |

Пример пуша в Docker Hub под другим тегом:
```bash
make devstack-build devstack-push TAG=v1.2.3
```

---

## Отличия от prod

| Аспект              | devstack                              | prod                        |
|---------------------|---------------------------------------|-----------------------------|
| SMS                 | MockSmsGateway, код = 123456          | Zvonok (реальные звонки)    |
| Платежи             | MockPaymentGateway (фейковый URL)     | T-Bank (реальные платежи)   |
| Rate limiting       | Отключён (NoOp)                       | Redis rate limiter          |
| Данные в БД         | Предзасеянные аккаунты + события      | Пусто                       |
| Scheduler           | Отключён                              | Включён                     |
| Swagger UI          | Включён                               | Отключён                    |
| Открытые порты      | :80 (nginx)                           | По конфигу инфраструктуры   |
