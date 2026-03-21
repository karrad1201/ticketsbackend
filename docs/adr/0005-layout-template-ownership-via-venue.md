# 0005. Layout Template Ownership Via Venue

## Status

Accepted

## Context

После привязки `Venue` и `Event` к `Organization` нужно определить ownership для `LayoutTemplate`.

У шаблона рассадки нет собственного жизненного цикла вне `VenueSpace`, поэтому возникает выбор:

- хранить отдельный `organizationId` в `LayoutTemplate`
- выводить владельца через `VenueSpace -> Venue`

## Decision

- `LayoutTemplate` не хранит отдельный `organizationId`
- владелец шаблона определяется через `VenueSpace`, к которому он привязан
- при создании `LayoutTemplate` use case проверяет:
  - что `VenueSpace` существует
  - что `Venue` этого пространства привязан к организации
  - что пользователь состоит в этой организации

## Consequences

- ownership шаблона остаётся однозначным без дублирования данных
- смена организации у `Venue` автоматически означает смену владельца шаблонов этого venue
- создание шаблонов подчиняется тем же membership-правилам, что и создание venue/event
