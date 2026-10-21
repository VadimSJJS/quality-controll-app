# Руководство по поддержке

## Быстрый старт

```bash
copy .env.example .env.local   # заполнить БД, затем:
.\mvnw.cmd spring-boot:run
```

Для отладки в `.env.local`: `SSL_ENABLED=false`, `SERVER_PORT=8443`.
Вход: табельный номер + пароль из `V_PERSONAL_STPC2`.

## Структура

```
src/main/java/com/vadimsjjs/qualitycontrollapp/
├── controller/          # REST (/api/*) + PageController (HTML) + advice/
├── dto/                 # DTO запросов, ответов, отчётов
├── entity/              # JPA-сущности (mapped by native SQL к БД)
├── repository/          # Spring Data JPA (JPQL + нативные SQL)
├── service/             # бизнес-логика, отчёты, импорт/экспорт
├── security/            # вход по табельному номеру, RoleChecker
├── config/              # SecurityConfig, Oracle11gDialect, EncodingFilter
└── aspect/              # аудит действий (AOP)
```

Шаблоны: `defects/` (журнал, форма), `reports/`, `charts/`, `directories/`, `layouts/base.html`.

## Известные проблемы

**N+1 запросы.** На странице журнала 20 записей → 44 SQL-запроса (20 к `REWORK_ACTION`, по 2–8 к каждой ассоциации). При росте данных будет тормозить.
Фиксы: `@BatchSize(size = 25)` на lazy-ассоциациях (быстро) или `JOIN FETCH` в JPQL (правильно).

**Два пути записи доработки.** Excel-импорт (`ExcelImportExportService`) пишет `reworkWeightTonnes` напрямую, а веб-форма (`NonconformingProductService.applyReworkActions`) сохраняет в `ReworkAction` и вычисляет `restoredWeightTonnes`/`reassignedWeightTonnes`. Импорт должен создавать `ReworkAction` так же, как и форма.

**show-sql=true** по умолчанию в `application.properties` — на продакшене раздувает логи. Перенести в профиль `dev`.

## Критические правила

- **Оборудование = номер стана.** `NonconformingProduct.resolveEquipment()` — используйте его везде, а не `getEquipmentKey()`. Значение может быть в `EQUIPMENT_KEY` (Excel) или `UNIT_NUMBER` (форма).
- **Фильтры продублированы.** `findWithFiltersNative` (журнал, ROWNUM-пагинация) и `findWithFilter` (отчёты, JPQL). Меняйте условия синхронно.
- **Oracle 11g.** Нет `LIMIT`/`FETCH FIRST`, нет поддержки Flyway Community Edition. Миграции — вручную через SQL*Plus, пагинация — через ROWNUM. `spring.jpa.hibernate.ddl-auto=none` — схему из приложения не менять.
- **Кодировка.** Данные перенесены из старой системы (CP1251 прочитался как UTF-8). Для исправления: `POST /api/seed/fix-encoding` (ADMIN).

## Как добавить поле в запись

1. Колонка в БД → `entity/NonconformingProduct.java` → DTO в `dto/`.
2. `NonconformingProductService`: `toEntity`, `updateEntity`, `toResponse`.
3. Фильтр: `DefectFilterDto` + оба запроса в `NonconformingProductRepository`.
4. Интерфейс: `add.html` (форма) + `list.html` (колонка + `renderTable`).

## Как добавить отчёт

1. DTO в `dto/` (или в `ReportDto`).
2. Метод в `ReportService` по образцу `getReportBySite` → `findDefects(...)` → группировка.
3. `@GetMapping` + `@PreAuthorize("@roleChecker.canView()")` в `ReportController`.
4. Экспорт: метод в `ReportExportService` + `/export/excel/...` в контроллере.
5. Секция на `reports/index.html`.

## Как добавить справочник

1. `entity/` + `repository/`.
2. REST-контроллер по образцу `DefectTypeController`.
3. Секция на `directories/index.html`.

## Отладка

- `spring.jpa.show-sql=true` — SQL в логах.
- Логи сервера — `logs/app.log`.
- При старте печатается эффективная SSL-конфигурация — там видно кейстор/пароль/алиас.

## Перед коммитом

- `.\mvnw.cmd compile` проходит.
- Журнал фильтруется, запись создаётся/редактируется, пару отчётов выгружаете в Excel.
- `.env` и `.env.local` в коммит не попали.
