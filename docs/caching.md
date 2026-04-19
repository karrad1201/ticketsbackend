# Caching Strategy

## Обзор

Проект использует **два механизма кэширования** через Spring Cache абстракцию:

1. **Caffeine** (in-process, локальный кэш) — используется по умолчанию для справочных данных (`categories.all`, `cities.all`, `layoutTemplates.byVenueSpaceId`). Кэши объявлены через `spring.cache.caffeine.spec` и `spring.cache.cache-names` в `application.yml`.

2. **Redis Cache** — используется для пользовательских и событийных данных (`events`, `favorites`). Активируется автоматически при `order-flow.persistence=jdbc` через бин `RedisCacheConfig`. При `order-flow.persistence=in-memory` (или если свойство не задано) вместо Redis используется `ConcurrentMapCacheManager` — fallback-реализация без TTL, предназначенная только для разработки и тестов.

> **Важно:** Caffeine и ConcurrentMapCacheManager — локальные in-process кэши. При горизонтальном масштабировании (несколько инстансов приложения) каждый инстанс имеет независимый кэш. Инвалидация кэша не распространяется между инстансами. Для multi-instance деплоя все кэши должны быть переведены на Redis Cache.

## Конфигурация

### Caffeine (application.yml)

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000
    cache-names:
      - categories.all
      - cities.all
      - layoutTemplates.byVenueSpaceId
```

Параметры `spec`:
- `maximumSize=1000` — максимальное количество записей во всех Caffeine-кэшах суммарно (Caffeine применяет лимит per-cache). Когда кэш достигает лимита, наименее используемые записи вытесняются автоматически (LRU-подобная политика).

> Явный `expireAfterWrite` в конфиге не задан. Это означает, что Caffeine-кэши не имеют TTL: записи живут до инвалидации через `@CacheEvict` или до перезапуска приложения.

### Redis Cache (RedisCacheConfig.kt)

Бин `redisCacheManager` создаётся при `order-flow.persistence=jdbc`:

| Имя кэша  | TTL        |
|-----------|------------|
| `events`  | 10 минут   |
| `discovery` | 3 минуты |
| `favorites` | 5 минут  |

Null-значения не кэшируются (`disableCachingNullValues()`). Сериализация значений — JSON через Jackson.

> Кэш `discovery` объявлен в `RedisCacheConfig` и `FallbackRedisCacheConfig`, но в текущей кодовой базе ни один метод не использует его через `@Cacheable`. Он зарезервирован для будущего использования.

## Кэши

| Имя кэша | Что хранит | TTL | Ключ | CacheManager |
|---|---|---|---|---|
| `events` | Объект `Event` по ID | 10 минут (Redis) / нет TTL (fallback) | `eventId` (UUID) | `redisCacheManager` |
| `favorites` | Список избранных `Event` пользователя | 5 минут (Redis) / нет TTL (fallback) | `userId` (UUID) | `redisCacheManager` |
| `categories.all` | Полный список `Category` | нет TTL | фиксированный (весь список) | default (Caffeine) |
| `cities.all` | Полный список `City` | нет TTL | фиксированный (весь список) | default (Caffeine) |
| `layoutTemplates.byVenueSpaceId` | Список `LayoutTemplate` по ID площадки | нет TTL | `venueSpaceId` (UUID) | default (Caffeine) |

## Аннотации в коде

### `EventService.getById`
- **Файл:** `application/service/EventService.kt`
- **Аннотация:** `@Cacheable(value = ["events"], cacheManager = "redisCacheManager", key = "#id")`
- **Что кэшируется:** объект `Event`, найденный по `id`
- **Когда срабатывает:** при каждом вызове `getById(id)`, если запись ещё не в кэше

### `EventService.update`
- **Файл:** `application/service/EventService.kt`
- **Аннотация:** `@CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#event.id")`
- **Что инвалидируется:** запись события по ключу `event.id`
- **Когда срабатывает:** после успешного сохранения обновлённого события

### `EventService.deleteById`
- **Файл:** `application/service/EventService.kt`
- **Аннотация:** `@CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#id")`
- **Что инвалидируется:** запись события по ключу `id`
- **Когда срабатывает:** после удаления события

### `CloseEventSalesUseCase.closeByOrganizer`
- **Файл:** `application/usecase/CloseEventSalesUseCase.kt`
- **Аннотация:** `@CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#eventId")`
- **Что инвалидируется:** запись события по ключу `eventId`
- **Когда срабатывает:** когда организатор вручную закрывает продажи на событие

### `CloseEventSalesUseCase.closeWhenStarted`
- **Файл:** `application/usecase/CloseEventSalesUseCase.kt`
- **Аннотация:** `@CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#eventId")`
- **Что инвалидируется:** запись события по ключу `eventId`
- **Когда срабатывает:** при автоматическом закрытии продаж по расписанию (событие уже началось)

### `FavoriteQueryService.listAllFavoriteEvents`
- **Файл:** `application/query/FavoriteQueryService.kt`
- **Аннотация:** `@Cacheable(value = ["favorites"], cacheManager = "redisCacheManager", key = "#userId")`
- **Что кэшируется:** полный список избранных событий (`List<Event>`) для пользователя `userId`
- **Когда срабатывает:** при первом запросе избранного для данного пользователя; последующие обращения через `listFavoriteEvents` обслуживаются из кэша (пагинация выполняется в памяти по кэшированному списку)

### `FavoriteEventService.add`
- **Файл:** `application/service/FavoriteEventService.kt`
- **Аннотация:** `@CacheEvict(value = ["favorites"], cacheManager = "redisCacheManager", key = "#userId")`
- **Что инвалидируется:** список избранного пользователя `userId`
- **Когда срабатывает:** когда пользователь добавляет событие в избранное

### `FavoriteEventService.remove`
- **Файл:** `application/service/FavoriteEventService.kt`
- **Аннотация:** `@CacheEvict(value = ["favorites"], cacheManager = "redisCacheManager", key = "#userId")`
- **Что инвалидируется:** список избранного пользователя `userId`
- **Когда срабатывает:** когда пользователь удаляет событие из избранного

### `CategoryRepository.findAll`
- **Файл:** `domain/repository/CategoryRepository.kt`
- **Аннотация:** `@Cacheable("categories.all")`
- **Что кэшируется:** полный список категорий
- **Когда срабатывает:** при первом обращении к списку категорий

### `CategoryRepository.save` и `CategoryRepository.deleteById`
- **Файл:** `domain/repository/CategoryRepository.kt`
- **Аннотация:** `@CacheEvict(cacheNames = ["categories.all"], allEntries = true)`
- **Что инвалидируется:** весь кэш `categories.all`
- **Когда срабатывает:** при создании/обновлении или удалении категории

### `CityRepository.findAll`
- **Файл:** `domain/repository/CityRepository.kt`
- **Аннотация:** `@Cacheable("cities.all")`
- **Что кэшируется:** полный список городов
- **Когда срабатывает:** при первом обращении к списку городов
- **Примечание:** явной инвалидации нет — список городов считается стабильным справочником

### `LayoutTemplateRepository.findByVenueSpaceId`
- **Файл:** `domain/repository/LayoutTemplateRepository.kt`
- **Аннотация:** `@Cacheable(value = ["layoutTemplates.byVenueSpaceId"], key = "#venueSpaceId")`
- **Что кэшируется:** список шаблонов планировки (`List<LayoutTemplate>`) по ID зала
- **Когда срабатывает:** при первом запросе шаблонов для данного `venueSpaceId`

### `LayoutTemplateRepository.save` и `LayoutTemplateRepository.deleteById`
- **Файл:** `domain/repository/LayoutTemplateRepository.kt`
- **Аннотация:** `@CacheEvict(cacheNames = ["layoutTemplates.byVenueSpaceId"], allEntries = true)`
- **Что инвалидируется:** весь кэш `layoutTemplates.byVenueSpaceId` (все залы сразу)
- **Когда срабатывает:** при создании/обновлении или удалении шаблона планировки

## Ограничения и trade-offs

### Local cache vs distributed

Caffeine-кэши и ConcurrentMapCacheManager-fallback — процессные кэши. При запуске нескольких инстансов приложения:
- каждый инстанс хранит свою копию данных;
- `@CacheEvict` инвалидирует запись только в локальном кэше того инстанса, который обработал запрос;
- остальные инстансы продолжают отдавать устаревшие данные до истечения TTL (для Redis-кэшей) или до следующего `@CacheEvict` на конкретном инстансе.

Redis-кэши (`events`, `favorites`) используют общее хранилище, поэтому при multi-instance деплое они корректно разделяют данные. Однако Caffeine-кэши (`categories.all`, `cities.all`, `layoutTemplates.byVenueSpaceId`) по-прежнему остаются локальными.

### Cache warming

Нет механизма предзагрузки (preload/warm-up). Первый запрос к любому кэшируемому ресурсу после старта приложения или после инвалидации всегда уходит в базу данных. При высокой нагрузке это может привести к cache stampede.

### Stale data window

Для Redis-кэшей данные могут устареть на время TTL:
- `events` — до 10 минут (данные события могут быть неактуальны, если другой инстанс обновил событие)
- `favorites` — до 5 минут

Для Caffeine-кэшей без TTL данные актуальны до явного `@CacheEvict`. На одном инстансе это безопасно; при нескольких инстансах — потенциально бесконечное устаревание для `cities.all` (нет `@CacheEvict`).

### `@CacheEvict` на интерфейсах репозиториев

Аннотации `@CacheEvict` / `@Cacheable` размещены непосредственно на методах интерфейсов `CategoryRepository`, `CityRepository`, `LayoutTemplateRepository`. Spring Cache обрабатывает их через AOP-прокси на уровне реализации, поэтому вызовы методов репозитория напрямую (минуя прокси, например, из того же бина) не будут перехвачены.

## Рекомендации для production

### Multi-instance деплой

При горизонтальном масштабировании (более одного инстанса) необходимо перевести Caffeine-кэши на Redis:

1. Убрать `spring.cache.type: caffeine` из `application.yml`.
2. Добавить `categories.all`, `cities.all`, `layoutTemplates.byVenueSpaceId` в `RedisCacheConfig` с соответствующим TTL.
3. Добавить `cacheManager = "redisCacheManager"` ко всем `@Cacheable`/`@CacheEvict` аннотациям в репозиториях.

Redis уже подключён к проекту (используется для distributed locking через `EventLockManager`), поэтому дополнительных зависимостей не требуется.

### TTL для городов

`cities.all` не имеет `@CacheEvict` — список городов никогда не инвалидируется явно. Если города могут добавляться в production без перезапуска приложения, необходимо либо добавить `@CacheEvict` в методы мутации `CityRepository`, либо задать TTL при переводе на Redis.
