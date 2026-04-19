# Changelog

Все значимые изменения проекта документируются здесь.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/),
версионирование следует [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] — 2026-04-19

### Добавлено

#### Аутентификация и пользователи
- Регистрация и аутентификация пользователей через SMS OTP (интеграция с zvonok.com)
- Атомарная проверка SMS-кода через `tryMarkUsed` — исключает race condition при одновременных запросах
- Logout и очистка истекших auth-токенов
- Rate limiting невалидных Bearer-токенов по IP (10 запросов/мин → 429)
- Spring Security: `UnauthorizedException` → 401, разграничение admin/organizer/user endpoints

#### Управление организациями
- Подача заявки на создание организации (`OrganizationApplication`)
- Admin approval flow: просмотр, аппрув и отклонение заявок
- Автоматическое создание организации и назначение владельца (`OrganizationMember`) при аппруве
- Управление балансом организации с атомарным кредитованием при успешной продаже
- Управление участниками организации

#### Каталог и discovery
- Управление площадками (`Venue`), пространствами (`VenueSpace`), шаблонами рассадки (`LayoutTemplate`)
- Управление категориями событий
- Создание и управление событиями (`Event`), привязка к организации, площадке и категории
- Discovery API: список событий, фильтрация по городу и категории, только события доступные для покупки
- Поиск событий через целевые repository query (без глобального `findAll`)
- Кеширование discovery с правильной инвалидацией на уровне пользователя
- История посещений событий (`UserEventVisit`)

#### Покупка билетов и inventory
- Покупка без рассадки (general admission): резервирование нужного количества мест на время оплаты
- Покупка с рассадкой (seated): hold конкретных мест до завершения оплаты
- Создание заказа (`Order`), hold inventory, подтверждение и истечение
- Выпуск билетов (`Ticket`) при успешной оплате
- Автоматическое закрытие продаж при наступлении времени события (`salesClosedAt`)
- Ручное закрытие продаж организатором
- Закрытие продаж события завершает все pending purchase attempts
- Batch-обработка просроченных платёжных попыток (`ProcessStalePaymentAttemptsUseCase`)
- Batch auto-close начавшихся событий (`ProcessStartedEventSalesUseCase`)
- Scheduler для автоматического запуска batch-процессов
- Admin HTTP surface для ручного запуска batch-процессов

#### Интеграция с платёжным шлюзом
- Интеграция с TBank (T-Bank) как основной платёжный шлюз
- `PaymentGateway` — абстракция для создания платёжной сессии
- `HandlePaymentCallbackUseCase` — обработка внешних confirm/fail callback'ов
- `PaymentCallbackAudit` — хранение и аудит входящих callback-событий
- `PaymentReconciliationService` — поиск устаревших pending платёжных попыток
- Mock callback endpoint `POST /api/payments/callbacks/mock` для тестового контура
- WireMock dev stubs для локальной разработки без реальной платёжки

#### QR-валидация билетов
- Валидация билетов по QR-коду с записью реального времени использования в БД

#### Инфраструктура и persistence
- JDBC + Flyway runtime как основной production contour (H2 для dev, PostgreSQL-совместимый SQL)
- 21 Flyway migration (V1–V21): схема, индексы, FOREIGN KEY constraints
- In-memory профиль для лёгкой локальной разработки без datasource
- Distributed locking через Redis (`RedisEventLockManager`) для конкурентного бронирования
- Rate limiting SMS и HTTP через Redis (атомарный INCR+EXPIRE через Lua-скрипт)
- Batch INSERT в `JdbcTicketRepository.saveAll()`
- Batch-загрузка seat keys и admission items в `JdbcOrderRepository`
- UPSERT во всех JDBC-репозиториях (вместо DELETE+INSERT)
- CORS настройка с поддержкой нескольких origins и credentials
- API versioning: все endpoints под `/api/v1/`
- Swagger UI отключён в production, Actuator настроен

### Исправлено

- **CORS**: разделение строки `allowed-origins` по запятой; поддержка credentials
- **Rate limiter**: использование `X-Forwarded-For` для определения реального IP клиента
- **Валидация билетов**: `ValidateTicket` возвращает реальное `usedAt` из БД при race condition
- **Платёжный gateway**: валидация суммы из callback против суммы заказа
- **Phantom holds**: компенсация при ошибке payment gateway — призрачные резервы корректно освобождаются
- **Redis lock release**: атомарный Lua-скрипт исключает release чужого лока
- **Изоляция транзакций**: `REPEATABLE_READ` для order flow — предотвращает аномалии при параллельных покупках
- **Deadlock в admission**: `SELECT FOR UPDATE` в `reserveAdmission` устраняет взаимоблокировки
- **Discovery cache eviction**: инвалидация кеша ограничена конкретным пользователем
- **MockSmsGateway**: защита от случайного запуска в prod через `@Profile("!prod")`
- **Mock payment endpoint**: защита от запуска в prod через `@Profile("!prod")`
- **N+1 запросы**: устранены в `PaymentReconciliationService`, `JdbcLayoutTemplateRepository`, `JdbcOrderRepository`
- **Атомарность баланса организации**: устранена race condition read-modify-write при кредитовании
- **Безграничный рост ConcurrentHashMap**: ограничен рост in-memory хранилищ
- **Транзакционность**: `PaymentSettlementService.completePaidOrder` обёрнут в транзакцию
- **Security**: проверка owner-check в `confirm-payment`, требование `requireAdmin` в `expire`
- **Security**: `SecurityException` → 403 при попытке закрыть продажи не-организатором
- **SQL pagination**: корректная работа пагинации в JDBC-запросах
- **Phone validation**: валидация номера телефона при регистрации
- **Avatar upload**: проверка прав при загрузке аватара
- **Discovery**: корректная передача `userId` как параметра запроса
- **Конфликт Flyway**: переименование `V10__performance_indexes` → `V21` для устранения конфликта версий

[Unreleased]: https://github.com/karrad1201/ticketsbackend/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/karrad1201/ticketsbackend/releases/tag/v0.1.0
