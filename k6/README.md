# k6 Load Tests

Нагрузочные тесты для `bilets` backend на базе [k6](https://k6.io/).

## Структура

```
k6/
├── load-test.js          # точка входа — объединяет все сценарии
├── config.js             # BASE_URL, профили нагрузки, SLO-пороги
├── helpers/
│   ├── auth.js           # утилиты регистрации / входа
│   └── setup.js          # подготовка тестовых данных (вызывается один раз)
└── scenarios/
    ├── browse.js         # публичные GET-эндпоинты (70 % трафика)
    ├── auth_flow.js      # send-code → register → me → logout → login (15 %)
    └── order_flow.js     # create order → confirm payment (15 %)
```

## Профили

| Профиль | VU (browse / auth / order) | Длительность | Назначение |
|---------|---------------------------|--------------|------------|
| `smoke` | 1 / 1 / 1                 | 30 с         | Проверка работоспособности |
| `load`  | до 100 / 20 / 15          | 5 мин        | Реалистичная нагрузка |
| `stress`| до 300 / — / 80           | 7 мин        | Поиск предела |

## SLO (Service Level Objectives)

| Метрика | Цель |
|---------|------|
| browse p95 latency | < 200 мс |
| auth p95 latency   | < 300 мс |
| order p95 latency  | < 800 мс |
| Глобальный error rate | < 1 % |

## Предварительные требования

* Бэкенд запущен (по умолчанию `http://localhost:8080`)
* PostgreSQL, Redis, WireMock запущены (`docker compose up -d`)
* В `application.properties` (или окружении) SMS-сервис принимает код `123456`
  (в тестовом профиле `FakeSmsCodeService` принимает любой 6-значный код)

## Запуск

### Локально (k6 установлен)

```bash
# smoke — быстрая проверка
k6 run -e PROFILE=smoke k6/load-test.js

# load — стандартная нагрузка
k6 run k6/load-test.js

# stress — поиск предела
k6 run -e PROFILE=stress k6/load-test.js

# против staging-окружения
k6 run -e BASE_URL=https://api.staging.example.com -e PROFILE=load k6/load-test.js
```

### Через Docker Compose

```bash
# Запуск инфраструктуры + k6 (профиль load)
docker compose -f docker-compose.yml -f docker-compose.k6.yml run k6

# smoke-прогон
PROFILE=smoke docker compose -f docker-compose.yml -f docker-compose.k6.yml run k6
```

### Установка k6 (macOS / Linux)

```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Windows (winget)
winget install k6
```

## Интерпретация результатов

После завершения теста k6 выводит сводную таблицу. Ключевые метрики:

* `http_req_duration` — задержка запросов (avg, p90, p95, p99)
* `http_req_failed` — доля ошибочных ответов (4xx/5xx и сетевые ошибки)
* `http_reqs` — общее количество запросов и RPS
* `checks` — процент прошедших проверок (`check()` в сценариях)

Тест считается **провалившимся** (exit code 99), если хотя бы один порог из
`config.js → THRESHOLDS` нарушен.
