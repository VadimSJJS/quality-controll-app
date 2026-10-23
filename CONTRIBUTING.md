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
├── enums/               # перечисления (ReworkActionType)
├── config/              # SecurityConfig, Oracle11gDialect, EncodingFilter
└── aspect/              # аудит действий (AOP)
```

Шаблоны: `defects/` (журнал, форма), `reports/`, `charts/`, `directories/`, `layouts/base.html`.

## Известные проблемы

**Марка стали хранится текстом.** В `NONCONFORMING_PRODUCT.STEEL_GRADE` лежит название, а не FK. Для ответа API `steelGradeId` ищется по справочнику (`steelGradeIdsByName()` — один запрос на страницу).

**Единый фильтр.** Все выборки несоответствующей продукции (журнал и отчёты) используют
`NonconformingProductSpecification.byFilter(...)`, поэтому условия фильтрации не дублируются.

**Коды действий по доработке.** Централизованы в `enums/ReworkActionType` (`RESTORED=1`, `REASSIGNED=2`, `SCRAPPED=3`); магические числа в сервисах не используются.

## Как добавить поле в запись

1. Колонка в БД → `entity/NonconformingProduct.java` → DTO в `dto/`.
2. `NonconformingProductService`: `toEntity`, `updateEntity`, `toResponse`.
3. Фильтр: `DefectFilterDto` + оба запроса в `NonconformingProductRepository`.
4. Интерфейс: `add.html` (форма) + `list.html` (колонка + `renderTable`).
## Визуальный интерфейс: импорт и экспорт в журнале несоответствий

На странице **Журнал несоответствий** (`/defects`) доступны следующие операции через панель действий:

### Загрузка шаблона для заполнения

- Нажмите кнопку **Шаблон** в панели инструментов
- Будет скачан файл `Шаблон_ввода_данных_по_несоответствующей_продукции.xlsx`
- Шаблон содержит структуру с 24 колонками (см. `ExcelImportExportService.COL_*`, номера колонок соответствуют Приложению А)
- Заполните файл данными, соблюдая порядок колонок и форматы дат (dd.MM.yyyy)

### Импорт из Excel

- На странице журнала есть область импорта с полем выбора файла и кнопкой "Загрузить"
- Поддерживаются форматы `.xlsx`
- При загрузке показывается результат: количество успешно импортированных записей и список ошибок (если есть)
- После успешного импорта страница автоматически перезагружается через 2.5 секунды
- Доступно пользователям с ролью, имеющей право редактирования

### Экспорт в Excel (выгрузка данных)

- Также на странице доступна выгрузка текущих данных в Excel
- Используется тот же формат, что и при импорте, но в режиме чтения-only

<details>
<summary>Кнопки и элементы интерфейса</summary>

**В списке несоответствий (`defects/list.html`):**
- Поле ввода файла: `<input type="file" id="fileInput">`
- Кнопка импорта: `<button id="importBtn">...</button>`
- Область результатов: `<div id="importResult">...</div>`

**В шаблоне:**
- 24 колонки с данными (дата обнаружения, участок, бригада, диаметр, код продукта, номер катушки, номер нагрева, сталь, оборудование, ключ работы, оператор, производитель бригады, количество, примечание, вес, тип дефекта, причина, подпричина, источник обнаружения, дата доработки, тип доработки, количество доработки, вес доработки, примечание доработки)
- Строка заголовков на 6-й строке (строка индекс 5, `DATA_START_ROW = 6`)
- Шаблон структурирован согласно Приложению А

</details>

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

- SQL в логах включается профилем `dev`: `SPRING_PROFILES_ACTIVE=dev` (или `--spring.profiles.active=dev`), см. `application-dev.properties`.
- Логи сервера — `logs/app.log`.
- При старте печатается эффективная SSL-конфигурация — там видно кейстор/пароль/алиас.

## Перед коммитом

- `.\mvnw.cmd compile` проходит.
- Журнал фильтруется, запись создаётся/редактируется, пару отчётов выгружаете в Excel.
- `.env` и `.env.local` в коммит не попали.
